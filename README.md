# Amazing™ for Paper (1.21.11)

[![Build Paper Plugin](https://github.com/Ellosan/amazing-mod/actions/workflows/build-paper.yml/badge.svg?branch=paper-1.21.11)](https://github.com/Ellosan/amazing-mod/actions/workflows/build-paper.yml)

**Earth's Blockiest Store as a server-side Paper plugin** — vanilla clients
can join, no mod installation needed. This is the Paper edition of the
[Amazing Fabric mod](https://github.com/Ellosan/amazing-mod) (see `main`).

## Features

- 🛒 **Shop GUI** — `/amazing shop`: 80+ products in a paged chest menu,
  paid in MineBank dollars, with Prime Exclusives
- 📦 **Courier deliveries** — orders are walked to you by an Amazing courier
  NPC who hands over a shulker **package** with your goods inside
- 💵 **MineBank** — `/bank` menu: balance, cash banknote withdrawals and
  deposits, emerald buy-back, `/pay <player> <amount>` transfers, and the
  **$20 / 30-in-game-days Prime** subscription
- 🗺️ **Quests** — right-click any courier: supply runs and express reviews,
  paid straight to your account
- 🏙️ **Amazing City world generator** — road grids, furnished houses (with
  pools), offices, apartment towers, parks, banks, cinemas, and rare
  warehouses stacked with **stocked barrels**; citizens included
- 🛗 **Lodestone elevators** — stand on a lodestone pad, jump to ride up,
  sneak to ride down (used by generated offices and towers)
- 📬 Ambient courier visits to villagers

## Installation

1. Drop `amazing-paper-1.0.0.jar` into your server's `plugins/` folder
   (Paper **1.21.11**, Java 21+).
2. Optional — for the city world, add to `bukkit.yml` **before** first start:
   ```yaml
   worlds:
     world:
       generator: AmazingPaper
   ```
3. Start the server. Press nothing — the couriers are already on their way.

## What the Fabric edition has that this can't

Drivable vans, the AmazingPhone, internet radio, custom blocks/textures and
the Amazing title screen need client-side code — see the Fabric mod on the
`main` branch for the full experience.

## Building

```bash
./gradlew build   # → build/libs/amazing-paper-1.0.0.jar
```

## License

MIT — see [LICENSE](LICENSE).
