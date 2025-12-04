import React, { useState, useEffect, useMemo } from 'react';
import { listen } from '@tauri-apps/api/event';
import {
  Container,
  Grid,
  Paper,
  Text,
  Title,
  Group,
  Center,
  Stack,
  ThemeIcon,
  Badge
} from '@mantine/core';
import { IconCpu, IconDeviceDesktopAnalytics, IconServer } from '@tabler/icons-react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from 'recharts';

// Data types matching the Rust struct
interface CpuData {
  currentLoad: number;
}

interface MemoryData {
  total: number;
  used: number;
  free: number;
  available: number;
}

interface ProcessData {
  platform: string;
  cpuModel: string;
  node: string;
  electron: string; // Used for "Tauri v2"
  chrome: string;   // Used for "WebView"
}

interface SystemStats {
  cpu: CpuData;
  memory: MemoryData;
  process: ProcessData;
}

// CPU Chart Data Type
interface CpuHistoryPoint {
  time: string;
  load: number;
}

const MAX_HISTORY = 60;

function formatBytes(bytes: number, decimals = 2) {
  if (!+bytes) return '0 Bytes';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

const App: React.FC = () => {
  const [systemData, setSystemData] = useState<SystemStats | null>(null);
  const [cpuHistory, setCpuHistory] = useState<CpuHistoryPoint[]>([]);

  useEffect(() => {
    // Listen to 'system-stats' event from Rust backend
    const unlistenPromise = listen<SystemStats>('system-stats', (event) => {
      const data = event.payload;
      setSystemData(data);

      const now = new Date().toLocaleTimeString();
      setCpuHistory((prev) => {
        const newHistory = [...prev, { time: now, load: data.cpu.currentLoad }];
        if (newHistory.length > MAX_HISTORY) {
          return newHistory.slice(newHistory.length - MAX_HISTORY);
        }
        return newHistory;
      });
    });

    return () => {
      unlistenPromise.then((unlisten) => unlisten());
    };
  }, []);

  // Format Memory Data
  const memoryData = useMemo(() => {
    if (!systemData) return [];
    return [
      { name: 'Used', value: systemData.memory.used },
      { name: 'Free', value: systemData.memory.free },
    ];
  }, [systemData]);

  const COLORS = ['#fa5252', '#40c057']; // Red for Used, Green for Free

  if (!systemData) {
    return (
      <Center h="100vh">
        <Text>Waiting for System Data...</Text>
      </Center>
    );
  }

  return (
    <Container fluid p="md" style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
        <Group mb="md">
            <ThemeIcon size="lg" variant="light" color="blue">
                <IconDeviceDesktopAnalytics />
            </ThemeIcon>
            <Title order={2}>System Monitor Dashboard</Title>
        </Group>

      <Grid gutter="md" style={{ flex: 1 }}>
        {/* CPU Section */}
        <Grid.Col span={{ base: 12, md: 8 }}>
          <Paper withBorder p="md" radius="md" h="100%">
            <Group mb="md">
                <ThemeIcon variant="light" color="grape"><IconCpu size={20}/></ThemeIcon>
                <Title order={4}>CPU Usage History (60s)</Title>
                <Badge color={systemData.cpu.currentLoad > 80 ? 'red' : 'blue'}>
                    {systemData.cpu.currentLoad.toFixed(1)}%
                </Badge>
            </Group>
            <div style={{ width: '100%', height: 'calc(100% - 50px)' }}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={cpuHistory}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#373A40" />
                  <XAxis dataKey="time" hide />
                  <YAxis domain={[0, 100]} />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#25262b', borderColor: '#373A40', color: 'white' }}
                    itemStyle={{ color: 'white' }}
                    formatter={(value: number) => [`${value.toFixed(1)}%`, 'Load']}
                  />
                  <Line
                    type="monotone"
                    dataKey="load"
                    stroke="#228be6"
                    strokeWidth={2}
                    dot={false}
                    isAnimationActive={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </Paper>
        </Grid.Col>

        {/* Memory Section */}
        <Grid.Col span={{ base: 12, md: 4 }}>
          <Paper withBorder p="md" radius="md" h="100%">
             <Group mb="md">
                <ThemeIcon variant="light" color="teal"><IconServer size={20}/></ThemeIcon>
                <Title order={4}>Memory Usage</Title>
            </Group>

            <Stack align="center" justify="center" h="calc(100% - 50px)">
                <div style={{ width: '100%', height: 200, position: 'relative' }}>
                    <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                        <Pie
                            data={memoryData}
                            cx="50%"
                            cy="50%"
                            innerRadius={60}
                            outerRadius={80}
                            fill="#8884d8"
                            paddingAngle={5}
                            dataKey="value"
                            startAngle={90}
                            endAngle={-270}
                        >
                            {memoryData.map((_, index) => (
                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                            ))}
                        </Pie>
                         <Tooltip
                            formatter={(value: number) => formatBytes(value)}
                            contentStyle={{ backgroundColor: '#25262b', borderColor: '#373A40', color: 'white' }}
                         />
                        </PieChart>
                    </ResponsiveContainer>
                    <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', textAlign: 'center' }}>
                         <Text size="xs" c="dimmed">Used</Text>
                         <Text fw={700}>{((systemData.memory.used / systemData.memory.total) * 100).toFixed(1)}%</Text>
                    </div>
                </div>

                <Stack gap="xs" align="center">
                    <Text size="lg" fw={500}>
                        {formatBytes(systemData.memory.used)} / {formatBytes(systemData.memory.total)}
                    </Text>
                    <Group gap="xs">
                        <Badge color="red" variant="dot">Used</Badge>
                        <Badge color="green" variant="dot">Free</Badge>
                    </Group>
                </Stack>
            </Stack>
          </Paper>
        </Grid.Col>

        {/* Info Section */}
        <Grid.Col span={12}>
            <Paper withBorder p="xs" radius="md" bg="dark.8">
                <Group justify="space-between" px="md">
                    <Text size="xs" c="dimmed">CPU Model: <Text span c="white">{systemData.process.cpuModel}</Text></Text>
                    <Group gap="lg">
                        <Text size="xs" c="dimmed">Backend: <Text span c="white">{systemData.process.node}</Text></Text>
                        <Text size="xs" c="dimmed">Framework: <Text span c="white">{systemData.process.electron}</Text></Text>
                        <Text size="xs" c="dimmed">Renderer: <Text span c="white">{systemData.process.chrome}</Text></Text>
                        <Text size="xs" c="dimmed">Platform: <Text span c="white">{systemData.process.platform}</Text></Text>
                    </Group>
                </Group>
            </Paper>
        </Grid.Col>
      </Grid>
    </Container>
  );
};

export default App;
