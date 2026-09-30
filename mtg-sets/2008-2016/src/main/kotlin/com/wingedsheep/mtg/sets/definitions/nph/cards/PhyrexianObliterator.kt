package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phyrexian Obliterator — New Phyrexia #68
 * {B}{B}{B}{B} · Creature — Phyrexian Horror · 5/5
 *
 * Trample
 * Whenever a source deals damage to this creature, that source's controller sacrifices that many
 * permanents of their choice.
 *
 * Same join as Belltower Sphinx: `Triggers.self.isDealtDamage()` binds the damage *source* as the
 * triggering entity, so `Player.ControllerOfTriggeringEntity` is "that source's controller" (last-known
 * controller for a burn spell that has already left the stack), and the amount is the per-event
 * damage. Reprinted in ONE (Printing row there).
 */
val PhyrexianObliterator = card("Phyrexian Obliterator") {
    manaCost = "{B}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Horror"
    power = 5
    toughness = 5
    oracleText = "Trample\nWhenever a source deals damage to this creature, that source's " +
        "controller sacrifices that many permanents of their choice."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.Sacrifice(
            GameObjectFilter.Any,
            DynamicAmounts.triggerDamageAmount(),
            EffectTarget.PlayerRef(Player.ControllerOfTriggeringEntity),
        )
        description = "Whenever a source deals damage to this creature, that source's controller " +
            "sacrifices that many permanents of their choice."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "68"
        artist = "Todd Lockwood"
        flavorText = "\"Behold blessed perfection.\"\n—Sheoldred, Whispering One"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44c4476d-58f9-420d-9545-f5d580c589de.jpg?1783941312"
        ruling(
            "2023-02-04",
            "If creatures an opponent controls are dealt lethal damage at the same time that Phyrexian " +
                "Obliterator is dealt damage, those creatures will be destroyed before that player " +
                "chooses permanents to sacrifice."
        )
    }
}
