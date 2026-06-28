package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cid, Timeless Artificer — Final Fantasy #216
 * {2}{W}{U} · Legendary Creature — Human Artificer · 4/4
 *
 * Artifact creatures and Heroes you control get +1/+1 for each Artificer you control and each
 * Artificer card in your graveyard.
 * A deck can have any number of cards named Cid, Timeless Artificer.
 * Cycling {W}{U}
 *
 * Modeling:
 * - The lord is a continuous static ([GrantDynamicStatsEffect]) over the union group
 *   "artifact creatures you control OR Heroes you control" (Hero is a creature subtype, so the
 *   second branch is `Creature.withSubtype("Hero")`). The two homogeneous `youControl` branches
 *   flatten into one `CardPredicate.Or` via the `or` infix on [GameObjectFilter].
 * - The per-step bonus is the live sum of (Artificers you control on the battlefield) +
 *   (Artificer cards in your graveyard), modeled with [DynamicAmount.Add] of an
 *   [DynamicAmount.AggregateBattlefield] and a graveyard [DynamicAmount.Count]. The same amount
 *   feeds both power and toughness (+N/+N). Cid itself is a Human Artificer (not an artifact
 *   creature and not a Hero), so it counts toward the bonus but is not buffed by its own ability.
 * - "A deck can have any number of cards named Cid, Timeless Artificer" is a deck-construction
 *   rule (CR 105 / no in-game effect); it is captured in oracleText only and intentionally has no
 *   mechanical modeling.
 * - Cycling {W}{U} via [KeywordAbility.cycling].
 */
val CidTimelessArtificer = card("Cid, Timeless Artificer") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Artificer"
    power = 4
    toughness = 4
    oracleText = "Artifact creatures and Heroes you control get +1/+1 for each Artificer you control " +
        "and each Artificer card in your graveyard.\n" +
        "A deck can have any number of cards named Cid, Timeless Artificer.\n" +
        "Cycling {W}{U} ({W}{U}, Discard this card: Draw a card.)"

    // Artifact creatures and Heroes you control get +1/+1 for each Artificer you control and each
    // Artificer card in your graveyard.
    staticAbility {
        val artificerCount = DynamicAmount.Add(
            DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype(Subtype.ARTIFICER),
            ),
            DynamicAmount.Count(
                Player.You,
                Zone.GRAVEYARD,
                GameObjectFilter.Any.withSubtype(Subtype.ARTIFICER),
            ),
        )
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter(
                GameObjectFilter.ArtifactCreature.youControl() or
                    GameObjectFilter.Creature.withSubtype("Hero").youControl(),
            ),
            powerBonus = artificerCount,
            toughnessBonus = artificerCount,
        )
    }

    // Cycling {W}{U}
    keywordAbility(KeywordAbility.cycling("{W}{U}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fb99393-d2b6-40a6-8de7-317efdc4c50b.jpg?1748706567"
    }
}
