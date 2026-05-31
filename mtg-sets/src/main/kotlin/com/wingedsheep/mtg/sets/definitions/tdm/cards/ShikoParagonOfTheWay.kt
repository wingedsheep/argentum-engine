package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Shiko, Paragon of the Way — Tarkir: Dragonstorm #223
 * {2}{U}{R}{W} · Legendary Creature — Spirit Dragon · 4/5
 *
 * Flying, vigilance
 * When Shiko enters, exile target nonland card with mana value 3 or less from your graveyard.
 * Copy it, then you may cast the copy without paying its mana cost. (A copy of a permanent
 * spell becomes a token.)
 *
 * Composed from atomic primitives rather than a bespoke executor — the "copy a card in a zone,
 * then cast the copy" pattern (Rule 707.12) is just three chained steps:
 *
 *   1. **Exile** the targeted graveyard card ([MoveToZoneEffect] → exile).
 *   2. **Copy it** in place ([Effects.CopyCardIntoCollection]) — the copy is a stack-style copy
 *      created in exile and published to the `copy` collection.
 *   3. **You may cast the copy** for free ([Effects.CastFromCollectionWithoutPayingCost] wrapped
 *      in [MayEffect]). The copy is cast through the normal machinery, so it picks targets/X,
 *      becomes a token if it's a permanent spell, and ceases to exist if it's an instant/sorcery
 *      (Rule 707.10). A copy that's declined or can't be cast is removed by the Rule 707.10a
 *      state-based action, leaving no phantom card in exile.
 */
val ShikoParagonOfTheWay = card("Shiko, Paragon of the Way") {
    manaCost = "{2}{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Legendary Creature — Spirit Dragon"
    power = 4
    toughness = 5
    oracleText = "Flying, vigilance\n" +
        "When Shiko enters, exile target nonland card with mana value 3 or less from your " +
        "graveyard. Copy it, then you may cast the copy without paying its mana cost. " +
        "(A copy of a permanent spell becomes a token.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        description = "When Shiko enters, exile target nonland card with mana value 3 or less " +
            "from your graveyard. Copy it, then you may cast the copy without paying its mana cost."
        val exiledCard = target(
            "target nonland card with mana value 3 or less from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Nonland.manaValueAtMost(3).ownedByYou(),
                    zone = Zone.GRAVEYARD,
                )
            )
        )
        effect = Effects.Composite(
            MoveToZoneEffect(exiledCard, Zone.EXILE),
            Effects.CopyCardIntoCollection(exiledCard, storeAs = "copy"),
            MayEffect(
                Effects.CastFromCollectionWithoutPayingCost("copy"),
                descriptionOverride = "You may cast the copy without paying its mana cost.",
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "223"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/8138cf10-1e3e-483f-86ad-cc399192657d.jpg?1743204881"
    }
}
