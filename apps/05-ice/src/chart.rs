use iced::mouse;
use iced::widget::canvas::{stroke, Cache, Geometry, Path, Program, Stroke};
use iced::{Color, Point, Rectangle, Theme};

// 차트 색상 정의 (Neon Colors)
const NEON_CYAN: Color = Color::from_rgb(0.0, 1.0, 1.0);
const NEON_MAGENTA: Color = Color::from_rgb(1.0, 0.0, 1.0);
const DARK_BG: Color = Color::from_rgb(0.1, 0.1, 0.1);

pub struct CpuUsageChart {
    pub data: Vec<f32>,
    pub cache: Cache,
}

impl CpuUsageChart {
    pub fn new(data: Vec<f32>) -> Self {
        Self {
            data,
            cache: Cache::new(),
        }
    }
}

impl<Message> Program<Message> for CpuUsageChart {
    type State = ();

    fn draw(
        &self,
        _state: &Self::State,
        renderer: &iced::Renderer,
        _theme: &Theme,
        bounds: Rectangle,
        _cursor: mouse::Cursor,
    ) -> Vec<Geometry> {
        let chart = self.cache.draw(renderer, bounds.size(), |frame| {
            // 배경 그리기
            frame.fill_rectangle(
                Point::ORIGIN,
                bounds.size(),
                DARK_BG,
            );

            if self.data.is_empty() {
                return;
            }

            // 라인 차트 그리기
            let data_points = self.data.len();
            let step_x = bounds.width / (data_points as f32 - 1.0).max(1.0);

            let path = Path::new(|b| {
                for (i, &usage) in self.data.iter().enumerate() {
                    let x = i as f32 * step_x;
                    let y = bounds.height * (1.0 - usage / 100.0);

                    if i == 0 {
                        b.move_to(Point::new(x, y));
                    } else {
                        b.line_to(Point::new(x, y));
                    }
                }
            });

            frame.stroke(
                &path,
                Stroke {
                    style: stroke::Style::Solid(NEON_CYAN),
                    width: 2.0,
                    ..Stroke::default()
                },
            );
        });

        vec![chart]
    }
}

pub struct MemoryUsageChart {
    pub used_memory: u64,
    pub total_memory: u64,
    pub cache: Cache,
}

impl MemoryUsageChart {
    pub fn new(used: u64, total: u64) -> Self {
        Self {
            used_memory: used,
            total_memory: total,
            cache: Cache::new(),
        }
    }
}

impl<Message> Program<Message> for MemoryUsageChart {
    type State = ();

    fn draw(
        &self,
        _state: &Self::State,
        renderer: &iced::Renderer,
        _theme: &Theme,
        bounds: Rectangle,
        _cursor: mouse::Cursor,
    ) -> Vec<Geometry> {
        let chart = self.cache.draw(renderer, bounds.size(), |frame| {
            let center = bounds.center();
            let radius = bounds.width.min(bounds.height) / 2.0 * 0.9;
            let width = radius * 0.2; // 도넛 두께

            let usage_ratio = if self.total_memory > 0 {
                self.used_memory as f32 / self.total_memory as f32
            } else {
                0.0
            };

            // Full Circle Background (Free)
            let background_path = Path::circle(center, radius);
            frame.stroke(
                &background_path,
                Stroke {
                    style: stroke::Style::Solid(Color::from_rgb(0.2, 0.2, 0.2)),
                    width,
                    ..Stroke::default()
                },
            );

            // Usage Arc
            if usage_ratio > 0.0 {
                let end_angle: f32 = usage_ratio * 360.0;
                let start_angle: f32 = -90.0; // 12시 방향부터 시작

                let usage_path = Path::new(|b| {
                    b.arc(iced::widget::canvas::path::Arc {
                        center,
                        radius,
                        start_angle: iced::Radians(start_angle.to_radians()),
                        end_angle: iced::Radians((start_angle + end_angle).to_radians()),
                    });
                });

                frame.stroke(
                    &usage_path,
                    Stroke {
                        style: stroke::Style::Solid(NEON_MAGENTA),
                        width,
                        line_cap: stroke::LineCap::Round,
                        ..Stroke::default()
                    },
                );
            }
        });

        vec![chart]
    }
}
