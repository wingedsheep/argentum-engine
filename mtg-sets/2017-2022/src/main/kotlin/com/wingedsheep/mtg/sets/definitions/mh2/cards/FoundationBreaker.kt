package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Foundation Breaker — Modern Horizons 2 #160
 * {3}{G} · Creature — Elemental · 2 / 2
 *
 * When this creature enters, you may destroy target artifact or enchantment.
 * Evoke {1}{G} (You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters.)
 *
 * Reclamation Sage's body on an evoke chassis. The enters trigger is the ordinary
 * [Triggers.EntersBattlefield]; the printed "you may" is written as the builder's `optional = true`,
 * which is shorthand the DSL lowers into a `Gate.MayDecide` around the effect — there is no
 * `optional` field on the `TriggeredAbility` model itself. The consent is asked at resolution, but
 * the target is still chosen when the ability goes on the stack (CR 603.3d), so with no artifact or
 * enchantment anywhere on the battlefield the ability is simply removed and never asks.
 *
 * [Targets.ArtifactOrEnchantment] is the shared "artifact or enchantment" requirement rather than a
 * hand-rolled disjunction, and [Effects.Destroy] is the destruction facade (a move to the graveyard
 * with `byDestruction`, so indestructible and regeneration are honoured) rather than a raw move.
 *
 * `evoke` is a first-class alternative-cost field on the card DSL (CR 702.74a): the engine offers it
 * as a second cast option and supplies the "when this permanent enters, if its evoke cost was paid,
 * sacrifice it" trigger itself, so nothing else is authored for it. Both enters triggers go on the
 * stack together and their controller orders them, and the destroy resolves either way — which is
 * the entire point of an evoked Foundation Breaker: {1}{G} to blow up an artifact.
 */
val FoundationBreaker = card("Foundation Breaker") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may destroy target artifact or enchantment.\n" +
        "Evoke {1}{G} (You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters.)"

    evoke = "{1}{G}"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val t = target("target", Targets.ArtifactOrEnchantment)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Yeong-Hao Han"
        flavorText = "The castle finally got vengeance for enduring years of royally bad taste."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f08381e-34f5-4d08-b737-8c37964719e0.jpg?1783926832"
    }
}
