package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Curious Cadaver — Murders at Karlov Manor #194
 * {2}{U}{B} · Creature — Zombie Detective · 3/1
 *
 * Flying
 * When you sacrifice a Clue, return this card from your graveyard to your hand.
 *
 * The recursion ability functions **only** from the graveyard (`triggerZone = Zone.GRAVEYARD`,
 * the Pyre Zombie / Shambling Cie'th shape) — the printed wording says "return this card from
 * your graveyard", so it does nothing while Curious Cadaver is on the battlefield.
 *
 * `YouSacrificeA` is the per-permanent form (`perPermanent = true`): sacrificing two Clues at
 * once triggers twice, matching CR 603.2's per-event reading of "when you sacrifice a Clue".
 * The second instance finds the card already in hand and does nothing.
 *
 * Sacrificing a Clue for a *cost* (its own "{2}, Sacrifice this token: Draw a card", or Demand
 * Answers' additional cost) still triggers this — costs are paid before the spell or ability
 * goes on the stack, and the trigger fires off the sacrifice event either way.
 */
val CuriousCadaver = card("Curious Cadaver") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie Detective"
    oracleText = "Flying\nWhen you sacrifice a Clue, return this card from your graveyard to your hand."
    power = 3
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouSacrificeA(GameObjectFilter.Artifact.withSubtype("Clue"))
        triggerZone = Zone.GRAVEYARD
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Peter Polach"
        flavorText = "\"When you said I could learn a lot from a corpse, I have to admit I wasn't " +
            "expecting it to lecture me on forensic procedure.\"\n—Kellan, to Ezrim"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/2893aef8-835d-4935-b532-d8670585e489.jpg?1783912854"

        ruling(
            "2024-02-02",
            "Some abilities trigger \"whenever you sacrifice a Clue\". Those abilities trigger " +
                "whenever you sacrifice a Clue for any reason, not just to activate a Clue's " +
                "activated ability."
        )
        ruling(
            "2024-02-02",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact " +
                "token. For example, you can sacrifice Wrench to pay for Alquist Proft, Master " +
                "Sleuth's activated ability."
        )
    }
}
