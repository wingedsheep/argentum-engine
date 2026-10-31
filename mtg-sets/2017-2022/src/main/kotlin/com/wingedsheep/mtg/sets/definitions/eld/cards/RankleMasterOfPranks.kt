package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rankle, Master of Pranks — {2}{B}{B} Legendary Creature — Faerie Rogue 3/3 (Throne of Eldraine #101).
 *
 * "Choose any number" is a three-mode modal with `minChooseCount = 0`, chosen as the trigger goes on
 * the stack (the Rankle and Torbran shape). Chosen modes resolve in printed order.
 *
 * - **Discard** follows its ruling exactly (the Liliana, Waker of the Dead pipeline): every player
 *   chooses a card from their own hand in APNAP order without anything leaving a hand, then all the
 *   chosen cards are discarded together in one move.
 * - **Lose 1 life and draw** — each player loses 1 life, then each player draws a card.
 * - **Sacrifice** — [Effects.Sacrifice] over [Player.Each]: each player chooses a creature in APNAP
 *   order, then all the chosen creatures are sacrificed at once.
 */
val RankleMasterOfPranks = card("Rankle, Master of Pranks") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Faerie Rogue"
    power = 3
    toughness = 3
    oracleText = "Flying, haste\n" +
        "Whenever Rankle deals combat damage to a player, choose any number —\n" +
        "• Each player discards a card.\n" +
        "• Each player loses 1 life and draws a card.\n" +
        "• Each player sacrifices a creature of their choice."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Modal(
            modes = listOf(
                mode("Each player discards a card") {
                    effect = Effects.Pipeline(descriptionOverride = "Each player discards a card.") {
                        val (toDiscard) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                            listOf(chooseExactly(1, hand, prompt = "Choose a card to discard"))
                        }
                        discard(toDiscard)
                    }
                },
                mode("Each player loses 1 life and draws a card") {
                    effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.Each)) then
                        Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each))
                },
                mode("Each player sacrifices a creature of their choice") {
                    effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.Each))
                },
            ),
            chooseCount = 3,
            minChooseCount = 0,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "101"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93c2c11d-dfc3-4ba9-8c0f-a98114090396.jpg?1783932634"

        ruling("2019-10-04", "You can choose a mode even if some or all players will be unaffected.")
        ruling(
            "2019-10-04",
            "For each chosen mode, perform that mode in its entirety before continuing on to the next chosen " +
                "mode in the order printed."
        )
        ruling(
            "2019-10-04",
            "As the first mode is performed, first the player whose turn it is chooses a card in hand without " +
                "revealing it, then each other player in turn order does the same. Then all the chosen cards " +
                "are discarded at the same time."
        )
        ruling(
            "2019-10-04",
            "As the third mode is performed, first the player whose turn it chooses a creature they control, " +
                "then each other player in turn order does the same, knowing the choices made before them. " +
                "Then all the chosen creatures are sacrificed at the same time."
        )
    }
}
