package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Restless Fortress
 * Land
 *
 * This land enters tapped.
 * {T}: Add {W} or {B}.
 * {2}{W}{B}: This land becomes a 1/4 white and black Nightmare creature until end of turn. It's
 *   still a land.
 * Whenever this land attacks, defending player loses 2 life and you gain 2 life.
 *
 * The white-black member of the Wilds of Eldraine "Restless" creature-land cycle (see
 * [RestlessBivouac]). The attack trigger is an intrinsic triggered ability of the land, not one
 * granted by the animate ability.
 *
 * The drain is two independent fixed amounts rather than a linked life-transfer: the printed text is
 * "loses 2 life and you gain 2 life", so you gain 2 even if the defending player's life loss is
 * prevented or replaced. [Player.DefendingPlayer] resolves per CR 802.2a through this land's own
 * attack assignment, so attacking a planeswalker drains that planeswalker's controller.
 */
val RestlessFortress = card("Restless Fortress") {
    typeLine = "Land"
    colorIdentity = "WB"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {W} or {B}.\n" +
        "{2}{W}{B}: This land becomes a 1/4 white and black Nightmare creature until end of turn. " +
        "It's still a land.\n" +
        "Whenever this land attacks, defending player loses 2 life and you gain 2 life."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}{B}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 1,
            toughness = 4,
            creatureTypes = setOf("Nightmare"),
            colors = setOf(Color.WHITE.name, Color.BLACK.name),
            duration = Duration.EndOfTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            Effects.LoseLife(2, EffectTarget.PlayerRef(Player.DefendingPlayer)),
            Effects.GainLife(2, EffectTarget.Controller),
        )
        description = "Whenever this land attacks, defending player loses 2 life and you gain 2 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/675213bb-28d7-460c-a4f3-950f5b9090af.jpg?1783915055"

        ruling(
            "2023-09-01",
            "If this becomes a creature because of an effect other than its own ability, its last " +
                "ability will still trigger whenever it attacks."
        )
    }
}
