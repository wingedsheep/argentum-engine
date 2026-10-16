package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Commander Sofia Daguerre — Fallout #15
 * {3}{W} · Legendary Creature — Human Pilot · 1/3
 *
 * Flash
 * Crash Landing — When Commander Sofia Daguerre enters, destroy up to one target legendary
 * permanent. That permanent's controller creates a Junk token.
 *
 * Same shape as Cityscape Leveler: the destroy-and-replace runs inside `ForEachTarget`, so
 * choosing no target creates no Junk (a bare `TargetController` would fall back to the ability's
 * controller), and "that permanent's controller" reads the destroyed permanent's last-known
 * controller — an indestructible target still hands its controller a Junk.
 */
val CommanderSofiaDaguerre = card("Commander Sofia Daguerre") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Pilot"
    power = 1
    toughness = 3
    oracleText = "Flash\n" +
        "Crash Landing — When Commander Sofia Daguerre enters, destroy up to one target legendary permanent. " +
        "That permanent's controller creates a Junk token. (It's an artifact with \"{T}, Sacrifice this token: " +
        "Exile the top card of your library. You may play that card this turn. Activate only as a sorcery.\")"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetFilter(GameObjectFilter.Permanent.legendary()), optional = true)
        effect = Effects.ForEachTarget(
            Effects.Destroy(EffectTarget.ContextTarget(0)),
            Effects.CreateJunk(controller = EffectTarget.TargetController),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Craig Elliott"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/712697a4-69b4-4237-9340-96f0e5a32229.jpg?1783912418"
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-03-08", "Some spells and abilities that create Junk tokens may require targets. If each target chosen is illegal as that spell or ability tries to resolve, it won't resolve and none of its effects happen. You won't create any Junk tokens.")
    }
}
