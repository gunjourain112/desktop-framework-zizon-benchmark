use iced::widget::{column, container, row, text, Canvas};
use iced::{executor, Application, Command, Element, Length, Settings, Subscription, Theme, Color};
use iced::time;
use std::collections::VecDeque;
use std::time::Duration;
use sysinfo::{CpuRefreshKind, MemoryRefreshKind, RefreshKind, System};

mod chart;
use chart::{CpuUsageChart, MemoryUsageChart};

pub fn main() -> iced::Result {
    SystemMonitor::run(Settings {
        antialiasing: true,
        window: iced::window::Settings {
            size: iced::Size::new(1280.0, 800.0),
            resizable: true,
            ..iced::window::Settings::default()
        },
        ..Settings::default()
    })
}

struct SystemMonitor {
    system: System,
    cpu_history: VecDeque<f32>,
    cpu_chart_cache: chart::CpuUsageChart,
    memory_chart_cache: chart::MemoryUsageChart,
}

#[derive(Debug, Clone, Copy)]
enum Message {
    Tick,
}

impl Application for SystemMonitor {
    type Executor = executor::Default;
    type Message = Message;
    type Theme = Theme;
    type Flags = ();

    fn new(_flags: ()) -> (Self, Command<Message>) {
        let mut system = System::new_with_specifics(
            RefreshKind::new()
                .with_cpu(CpuRefreshKind::everything())
                .with_memory(MemoryRefreshKind::everything()),
        );
        system.refresh_cpu();
        system.refresh_memory();

        let cpu_history: VecDeque<f32> = vec![0.0; 60].into();

        (
            SystemMonitor {
                system,
                cpu_history: cpu_history.clone(),
                cpu_chart_cache: CpuUsageChart::new(cpu_history),
                memory_chart_cache: MemoryUsageChart::new(0, 100),
            },
            Command::none(),
        )
    }

    fn title(&self) -> String {
        String::from("System Monitor (Rust/Iced)")
    }

    fn update(&mut self, message: Message) -> Command<Message> {
        match message {
            Message::Tick => {
                self.system.refresh_cpu();
                self.system.refresh_memory();

                let global_cpu = self.system.global_cpu_info().cpu_usage();

                self.cpu_history.push_back(global_cpu);
                if self.cpu_history.len() > 60 {
                    self.cpu_history.pop_front();
                }

                self.cpu_chart_cache.data = self.cpu_history.clone();
                self.cpu_chart_cache.cache.clear();

                let used = self.system.used_memory();
                let total = self.system.total_memory();
                self.memory_chart_cache.used_memory = used;
                self.memory_chart_cache.total_memory = total;
                self.memory_chart_cache.cache.clear();

                Command::none()
            }
        }
    }

    fn view(&self) -> Element<'_, Message> {
        let global_cpu = self.system.global_cpu_info().cpu_usage();
        let used_mem = self.system.used_memory() as f64 / 1024.0 / 1024.0 / 1024.0; // GB
        let total_mem = self.system.total_memory() as f64 / 1024.0 / 1024.0 / 1024.0; // GB
        let mem_usage_percent = if self.system.total_memory() > 0 {
            (self.system.used_memory() as f64 / self.system.total_memory() as f64) * 100.0
        } else {
            0.0
        };

        let title = text("System Dashboard").size(40).style(Color::WHITE);

        let cpu_label = text(format!("CPU Usage: {:.1}%", global_cpu)).size(20).style(Color::WHITE);
        let cpu_chart_view = Canvas::new(&self.cpu_chart_cache)
            .width(Length::Fill)
            .height(Length::Fill);

        let cpu_container = container(
            column![
                cpu_label,
                container(cpu_chart_view)
                    .width(Length::Fill)
                    .height(Length::Fill)
                    .style(|_theme: &Theme| container::Appearance {
                        background: Some(iced::Background::Color(iced::Color::from_rgb(0.15, 0.15, 0.15))),
                        border: iced::Border {
                            radius: 10.0.into(),
                            width: 1.0,
                            color: iced::Color::from_rgb(0.3, 0.3, 0.3),
                        },
                        ..container::Appearance::default()
                    })
                    .padding(10)
            ]
            .spacing(10)
        )
        .width(Length::FillPortion(2))
        .height(Length::Fill)
        .padding(20);

        let mem_label = text(format!("Memory Usage\n{:.1} GB / {:.1} GB ({:.1}%)", used_mem, total_mem, mem_usage_percent))
            .size(20)
            .style(Color::WHITE)
            .vertical_alignment(iced::alignment::Vertical::Center)
            .horizontal_alignment(iced::alignment::Horizontal::Center);

        let mem_chart_view = Canvas::new(&self.memory_chart_cache)
            .width(Length::Fill)
            .height(Length::Fill);

        let mem_container = container(
            column![
                container(
                    column![
                        container(mem_chart_view).width(Length::Fill).height(Length::FillPortion(3)),
                        container(mem_label).width(Length::Fill).height(Length::FillPortion(1)).center_x()
                    ]
                )
                .width(Length::Fill)
                .height(Length::Fill)
                 .style(|_theme: &Theme| container::Appearance {
                        background: Some(iced::Background::Color(iced::Color::from_rgb(0.15, 0.15, 0.15))),
                        border: iced::Border {
                            radius: 10.0.into(),
                            width: 1.0,
                            color: iced::Color::from_rgb(0.3, 0.3, 0.3),
                        },
                        ..container::Appearance::default()
                    })
                    .padding(10)
            ]
            .spacing(10)
        )
        .width(Length::FillPortion(1))
        .height(Length::Fill)
        .padding(20);

        let content = column![
            container(title).padding(20),
            row![cpu_container, mem_container].spacing(20).height(Length::Fill)
        ]
        .spacing(0);

        container(content)
            .width(Length::Fill)
            .height(Length::Fill)
            .style(|_theme: &Theme| container::Appearance {
                background: Some(iced::Background::Color(iced::Color::from_rgb(0.05, 0.05, 0.05))),
                ..container::Appearance::default()
            })
            .into()
    }

    fn subscription(&self) -> Subscription<Message> {
        time::every(Duration::from_millis(1000)).map(|_| Message::Tick)
    }

    fn theme(&self) -> Theme {
        Theme::Dark
    }
}
