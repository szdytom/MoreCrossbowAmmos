# MoreCrossbowAmmos

A Minecraft mod that adds more vanilla items shootable from crossbows. This mod is made with Fabric, and it only needs to be installed on the server side.

## Features

### New Crossbow Projectiles

| Item 		 | Entity          | Need Offhand Held? | Durability Cost |
|---------------|-----------------|--------------------|-----------------|
| Snowball      | Snowball 	 | No                 | 1               |
| Egg           | Egg             | No                 | 1               |
| Wind Charge  | Wind Charge     | No                 | 1               |
| Fire Charge  | Fireball        | No                 | 5               |
| Ender Pearl  | Ender Pearl     | No				 | 1			   |
| Bottle o' Enchanting | Experience Bottle | No				 | 1			   |
| Splash Potion | Splash Potion   | No				 | 1			   |
| Lingering Potion | Lingering Potion | No				 | 1			   |
| Trident       | Trident         | Yes                | 3               |
| Blaze Powder | Small Fireball  | Yes				| 3               |

### New Game Rules

- `crossbowFireballPower`: The explosion power of fireballs shot from crossbows using "Fire Charge" as ammo. Default is `2`, can be set to any integer between `1` and `100`. (Warning: Setting this to a high value may cause significant server lag and/or crashes.)

### Miscellaneous

- Firework rockets that can explode now no longer need to be held in the offhand to shoot them from crossbows. You can now shoot firework rockets directly from your inventory (as long as these rockets can explode).
