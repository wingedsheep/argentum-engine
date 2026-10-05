package com.wingedsheep.mtg.sets.definitions.pcy.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rhystic Study
 * {2}{U}
 * Enchantment
 * Whenever an opponent casts a spell, you may draw a card unless that player pays {1}.
 *
 * The caster ([Player.TriggeringPlayer]) is offered the {1} when the trigger resolves; only if they
 * don't pay does the "you may draw" reach Rhystic Study's controller. The `May` sits *inside* the
 * suffer half, so per the ruling the draw decision is made after the payment decision — never before.
 */
val RhysticStudy = card("Rhystic Study") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent casts a spell, you may draw a card unless that player pays {1}."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana("{1}"),
            suffer = Effects.May(Effects.DrawCards(1)),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            consequenceDescription = "let Rhystic Study's controller draw a card",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Terese Nielsen"
        flavorText = "Friends teach what you want to know. Enemies teach what you *need* to know."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3394cefd-a3c6-4917-8f46-234e441ecfb6.jpg?1783945789"
        ruling(
            "2023-09-01",
            "Rhystic Study's triggered ability resolves before the spell that caused it to trigger. " +
                "It resolves even if that spell is countered. The player gets the option to pay when " +
                "this triggered ability resolves."
        )
        ruling(
            "2023-09-01",
            "You don't have to decide whether or not to draw a card until after the player decides " +
                "whether or not to pay."
        )
    }
}
