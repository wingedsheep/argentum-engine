package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.basicLand

/** One BEMOCS art variant (TMT 191-195) per basic-land type — the deckbuilder offers one entry per name. */

val TmtPlains = basicLand("Plains") {
    collectorNumber = "191"
    artist = "BEMOCS"
    imageUri = "https://cards.scryfall.io/normal/front/1/b/1b405611-27d4-435a-8e8e-6d528626fd27.jpg?1771343562"
}

val TmtIsland = basicLand("Island") {
    collectorNumber = "192"
    artist = "BEMOCS"
    imageUri = "https://cards.scryfall.io/normal/front/b/e/be2319a6-d089-4d13-8a7b-3552e654cdc5.jpg?1771343569"
}

val TmtSwamp = basicLand("Swamp") {
    collectorNumber = "193"
    artist = "BEMOCS"
    imageUri = "https://cards.scryfall.io/normal/front/4/1/41c7688d-8155-45fd-83ba-ff9be4d414a3.jpg?1771343575"
}

val TmtMountain = basicLand("Mountain") {
    collectorNumber = "194"
    artist = "BEMOCS"
    imageUri = "https://cards.scryfall.io/normal/front/6/2/6243f79f-b0e3-4fba-b899-7535e1a277e2.jpg?1771343582"
}

val TmtForest = basicLand("Forest") {
    collectorNumber = "195"
    artist = "BEMOCS"
    imageUri = "https://cards.scryfall.io/normal/front/6/1/6194850b-d70a-4f3e-be3e-bfb23ce1170b.jpg?1771343588"
}
