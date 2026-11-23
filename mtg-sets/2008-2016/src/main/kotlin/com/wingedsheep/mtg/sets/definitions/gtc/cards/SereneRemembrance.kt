package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serene Remembrance
 * {G}
 * Sorcery
 * Shuffle Serene Remembrance and up to three target cards from a single graveyard into their
 * owners' libraries.
 *
 * "From a single graveyard" is the cross-target `sameOwner` constraint (Shred Memory's shape);
 * "up to three" is `count = 3, optional = true`, so zero targets is legal and the spell still
 * shuffles itself in on resolution. The self-shuffle is the CR 608.2n destination replacement
 * ([selfShuffleIntoLibrary], Green Sun's Zenith), so a countered or fizzled spell goes to the
 * graveyard and a flashbacked one is exiled — exactly the card's rulings.
 */
val SereneRemembrance = card("Serene Remembrance") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Shuffle Serene Remembrance and up to three target cards from a single graveyard " +
        "into their owners' libraries."

    spell {
        targets(TargetFilter.CardInGraveyard, count = 3, optional = true, sameOwner = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.LIBRARY, placement = ZonePlacement.Shuffled)
        )
        selfShuffleIntoLibrary()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "David Palumbo"
        flavorText = "While Lazav schemed and Niv-Mizzet researched, Trostani sought the future in " +
            "the whispers of the past."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f3da4b8-9dd6-455d-b24a-3d0207ae5ee8.jpg?1783940115"
        ruling(
            "2013-01-24",
            "You may choose zero targets when you cast Serene Remembrance. If you do, you'll shuffle " +
                "Serene Remembrance into its owner's library when it resolves."
        )
        ruling(
            "2013-01-24",
            "If you choose at least one target for Serene Remembrance, and all of its targets are " +
                "illegal when Serene Remembrance tries to resolve, it won't resolve and none of its " +
                "effects will happen. You won't shuffle any library and Serene Remembrance will be put " +
                "into its owner's graveyard."
        )
        ruling(
            "2013-01-24",
            "If you give Serene Remembrance the flashback ability and cast it from your graveyard, it " +
                "will be exiled instead of being shuffled into your library. Any other targets will " +
                "still be shuffled into their owners' library, however."
        )
    }
}
