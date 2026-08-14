package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Projektor Inspector — Murders at Karlov Manor #68
 * {2}{U} · Creature — Human Detective · 3/2
 *
 * Whenever this creature or another Detective you control enters and whenever a Detective you
 * control is turned face up, you may draw a card. If you do, discard a card.
 *
 * The blue common that turns MKM's Detective tribal into card selection. A 3/2 for three that loots
 * on arrival and loots again every time the Detective count goes up.
 *
 * As with [PerimeterEnforcer], the printed ability is one ability with **two** trigger conditions,
 * modelled as two `triggeredAbility` blocks sharing an effect. That's faithful rather than a
 * compromise: no single event can satisfy both conditions, because turning a permanent face up is
 * not entering the battlefield (CR 707.9a).
 *
 * The "enters" half is [TriggerBinding.ANY], not `OTHER` — the printed text is "this creature **or
 * another** Detective you control", so the Inspector's own arrival loots. The filter is
 * Detective-you-control, which the Inspector satisfies itself.
 *
 * The "turned face up" half filters on the permanent's **post-flip** characteristics: a face-down
 * creature is a nameless, typeless 2/2, so a check against the face-down state would never see a
 * Detective. Practically this is the disguise payoff — flipping [GraniteWitness] or any other MKM
 * Detective loots as well.
 *
 * "You may draw a card. If you do, discard a card." is the coupled loot, so declining draws nothing
 * *and* discards nothing — [MayEffect] over `Patterns.Hand.loot()`, never two independent effects.
 */
val ProjektorInspector = card("Projektor Inspector") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Detective"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature or another Detective you control enters and whenever a " +
        "Detective you control is turned face up, you may draw a card. If you do, discard a card."

    // Whenever this creature or another Detective you control enters …
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.DETECTIVE).youControl(),
            binding = TriggerBinding.ANY
        )
        effect = MayEffect(Patterns.Hand.loot())
        description = "Whenever this creature or another Detective you control enters, you may " +
            "draw a card. If you do, discard a card."
    }

    // … and whenever a Detective you control is turned face up.
    triggeredAbility {
        trigger = Triggers.CreatureTurnedFaceUp(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.DETECTIVE)
        )
        effect = MayEffect(Patterns.Hand.loot())
        description = "Whenever a Detective you control is turned face up, you may draw a card. " +
            "If you do, discard a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Leonardo Santanna"
        flavorText = "\"Bring up the particulars from yesterday's altercation, and enhance.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad378843-e2b0-48d6-90dc-b584e857473d.jpg?1783912907"
    }
}
