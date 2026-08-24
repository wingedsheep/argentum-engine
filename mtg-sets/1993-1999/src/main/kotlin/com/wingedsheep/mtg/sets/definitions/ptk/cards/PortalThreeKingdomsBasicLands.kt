package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.basicLand

/**
 * Portal Three Kingdoms Basic Lands
 *
 * Portal Three Kingdoms contains 3 art variants of each basic land type.
 * Cards 166-180 (Plains 166-168, Island 169-171, Swamp 172-174, Mountain 175-177, Forest 178-180)
 */

// =============================================================================
// Plains (Cards 166-168)
// =============================================================================

val PortalThreeKingdomsPlains166 = basicLand("Plains") {
    collectorNumber = "166"
    artist = "He Jiancheng"
    imageUri = "https://cards.scryfall.io/normal/front/e/d/ed82f466-ee1c-4942-9b4d-a8bffd548b30.jpg"
}

val PortalThreeKingdomsPlains167 = basicLand("Plains") {
    collectorNumber = "167"
    artist = "He Jiancheng"
    imageUri = "https://cards.scryfall.io/normal/front/2/f/2fe3d8c0-1640-42bb-bb15-0b82c553976f.jpg"
}

val PortalThreeKingdomsPlains168 = basicLand("Plains") {
    collectorNumber = "168"
    artist = "He Jiancheng"
    imageUri = "https://cards.scryfall.io/normal/front/4/8/4801e24d-7c45-4d23-9461-0c7771eddd91.jpg"
}

// =============================================================================
// Island (Cards 169-171)
// =============================================================================

val PortalThreeKingdomsIsland169 = basicLand("Island") {
    collectorNumber = "169"
    artist = "Ku Xueming"
    imageUri = "https://cards.scryfall.io/normal/front/7/6/76033c90-1e06-473b-ba44-a94e16ac5348.jpg"
}

val PortalThreeKingdomsIsland170 = basicLand("Island") {
    collectorNumber = "170"
    artist = "Ku Xueming"
    imageUri = "https://cards.scryfall.io/normal/front/8/9/899cfba6-9131-43ef-9e8d-803a0e64d0b0.jpg"
}

val PortalThreeKingdomsIsland171 = basicLand("Island") {
    collectorNumber = "171"
    artist = "Ku Xueming"
    imageUri = "https://cards.scryfall.io/normal/front/e/7/e74b6430-0ddb-4433-b3bc-6f0ab42560e6.jpg"
}

// =============================================================================
// Swamp (Cards 172-174)
// =============================================================================

val PortalThreeKingdomsSwamp172 = basicLand("Swamp") {
    collectorNumber = "172"
    artist = "Wang Chuxiong"
    imageUri = "https://cards.scryfall.io/normal/front/1/7/17b4a8a4-caa8-472d-bf90-ee70250bc0ab.jpg"
}

val PortalThreeKingdomsSwamp173 = basicLand("Swamp") {
    collectorNumber = "173"
    artist = "Wang Chuxiong"
    imageUri = "https://cards.scryfall.io/normal/front/9/f/9fc74272-8dc7-4a07-85e3-e7a13573345e.jpg"
}

val PortalThreeKingdomsSwamp174 = basicLand("Swamp") {
    collectorNumber = "174"
    artist = "Wang Chuxiong"
    imageUri = "https://cards.scryfall.io/normal/front/7/a/7a6447bb-fffc-41c8-91cb-e526ed727b3e.jpg"
}

// =============================================================================
// Mountain (Cards 175-177)
// =============================================================================

val PortalThreeKingdomsMountain175 = basicLand("Mountain") {
    collectorNumber = "175"
    artist = "Qin Jun"
    imageUri = "https://cards.scryfall.io/normal/front/9/5/95046649-bc6d-46f5-8dbc-85d3fa5097c4.jpg"
}

val PortalThreeKingdomsMountain176 = basicLand("Mountain") {
    collectorNumber = "176"
    artist = "Qin Jun"
    imageUri = "https://cards.scryfall.io/normal/front/8/0/80a6f28b-6910-416e-86e1-bd428360bb27.jpg"
}

val PortalThreeKingdomsMountain177 = basicLand("Mountain") {
    collectorNumber = "177"
    artist = "Qin Jun"
    imageUri = "https://cards.scryfall.io/normal/front/0/0/00f0ab3f-86c5-49f6-948b-ace35bc03889.jpg"
}

// =============================================================================
// Forest (Cards 178-180)
// =============================================================================

val PortalThreeKingdomsForest178 = basicLand("Forest") {
    collectorNumber = "178"
    artist = "Ji Yong"
    imageUri = "https://cards.scryfall.io/normal/front/6/6/6677baa6-a38f-48f2-9ff2-2e3321b22959.jpg"
}

val PortalThreeKingdomsForest179 = basicLand("Forest") {
    collectorNumber = "179"
    artist = "Ji Yong"
    imageUri = "https://cards.scryfall.io/normal/front/c/5/c5c94d6c-ed6d-4544-8565-35f1ea8bfc26.jpg"
}

val PortalThreeKingdomsForest180 = basicLand("Forest") {
    collectorNumber = "180"
    artist = "Ji Yong"
    imageUri = "https://cards.scryfall.io/normal/front/8/4/84722185-dffb-498e-8ce7-f50236b716f5.jpg"
}

/**
 * All Portal Three Kingdoms basic land variants.
 */
val PortalThreeKingdomsBasicLands = listOf(
    PortalThreeKingdomsPlains166, PortalThreeKingdomsPlains167, PortalThreeKingdomsPlains168,
    PortalThreeKingdomsIsland169, PortalThreeKingdomsIsland170, PortalThreeKingdomsIsland171,
    PortalThreeKingdomsSwamp172, PortalThreeKingdomsSwamp173, PortalThreeKingdomsSwamp174,
    PortalThreeKingdomsMountain175, PortalThreeKingdomsMountain176, PortalThreeKingdomsMountain177,
    PortalThreeKingdomsForest178, PortalThreeKingdomsForest179, PortalThreeKingdomsForest180
)
