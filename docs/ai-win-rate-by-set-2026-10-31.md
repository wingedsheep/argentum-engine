# Engine AI vs humans — win rate per set (prod, 2026-10-31)

Source: `GET /api/stats/admin/ai-vs-human?days=<0|30>&set=<CODE>` on magic.wingedsheep.com.

**Overall:** 4,690 AI-vs-human games all time, AI win rate 34.8% (95% CI 33–36%).
Last 30 days: 1,927 games, 38.2% (36–40%).

Only lobby games (draft, sealed, tournament) record a set: about 1,700 of the 4,690 games.
Quick and constructed games are not in the table below. A lobby that used several sets counts
once for each of them.

## Per set

Range is the 95% confidence interval on the all-time AI win rate.

| Set | Games (all time) | AI win % (range) | Last 30 days: games | Last 30 days: AI win % |
|---|---:|---|---:|---:|
| FRA Reality Fracture | 484 | **24.0** (20–28) | 472 | 23.5 |
| ECL Lorwyn Eclipsed | 351 | 43.7 (39–49) | 164 | **54.0** |
| SOS Secrets of Strixhaven | 164 | 38.4 (31–46) | 142 | 33.1 |
| HOB The Hobbit | 109 | 47.7 (39–57) | 17 | 52.9 |
| J22 Jumpstart 2022 | 68 | 54.4 (43–66) | 68 | 54.4 |
| FDN Foundations | 64 | 48.4 (37–60) | 57 | 49.1 |
| FIN Final Fantasy | 47 | 40.4 (28–55) | 30 | 33.3 |
| MSH Marvel Super Heroes | 46 | 39.1 (26–54) | 38 | 31.6 |
| EOE Edge of Eternities | 45 | 33.3 (21–48) | 38 | 28.9 |
| SPM Marvel's Spider-Man | 25 | 20.0 (9–39) | 2 | – |
| TDM Tarkir: Dragonstorm | 23 | 52.2 (33–71) | 9 | 44.4 |
| TLA Avatar: The Last Airbender | 22 | 22.7 (10–43) | 7 | 42.9 |
| TMT Teenage Mutant Ninja Turtles | 22 | 27.3 (13–48) | 21 | 23.8 |
| RAV Ravnica: City of Guilds | 21 | 57.1 (37–76) | 10 | 50.0 |
| BLB Bloomburrow | 20 | 50.0 (30–70) | 9 | 55.6 |
| INR Innistrad Remastered | 18 | 50.0 (29–71) | 4 | 25.0 |

Every other set has 15 or fewer AI-vs-human games.

## Effect of the busiest players

All time, with the single busiest human in that set left out (`excludeTopHumans=1`):

| Set | AI win % | Without busiest human |
|---|---:|---:|
| FRA | 24.0 (484 games) | 26.9 (341 games) |
| SOS | 38.4 (164 games) | 46.8 (77 games) |
| ECL | 43.7 (351 games) | 53.9 (233 games) |

## Takeaways

- **FRA is a real weak spot, not one strong player.** Two players account for 262 of its 484
  games and beat the AI about 80% of the time. With the busiest one left out, the AI still wins
  only 26.9%.
- **SOS and ECL are skewed by one player each.** Without the busiest one, the AI wins 47% in
  SOS and 54% in ECL.
- **ECL got better recently:** 54% over the last 30 days, against 44% all time.
- **Also weak, but on fewer games:** EOE, TMT and MSH are at about 24–33% over the last 30 days.
  SPM and TLA look low, but too few games to read much into.
