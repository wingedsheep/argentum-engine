package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Kozilek, the Broken Reality
 * {9}
 * Legendary Creature — Eldrazi
 * 9/9
 * When you cast this spell, up to two target players each manifest two cards from their hands. For
 * each card manifested this way, you draw a card.
 * Other colorless creatures you control get +3/+2.
 *
 * The cast trigger iterates the chosen players with `forEachPlayerCollecting`: inside each pass
 * `Player.You` is the targeted player, who picks two cards from their own hand (or their only card —
 * a short hand yields what it holds) and manifests them face down under their own control. Each
 * pass records what actually moved, and the aggregate count outside the loop is the number of
 * cards "you" (the trigger's controller, restored after the loop) draw.
 */
val KozilekTheBrokenReality = card("Kozilek, the Broken Reality") {
    manaCost = "{9}"
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi"
    power = 9
    toughness = 9
    oracleText = "When you cast this spell, up to two target players each manifest two cards from " +
        "their hands. For each card manifested this way, you draw a card. (To manifest a card, put " +
        "it onto the battlefield face down as a 2/2 creature. Turn it face up any time for its mana " +
        "cost if it's a creature card.)\n" +
        "Other colorless creatures you control get +3/+2."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        target(TargetPlayer(count = 2, optional = true))
        effect = Effects.Pipeline {
            val (allManifested) = forEachPlayerCollecting(Player.EachTargetedPlayer) {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val chosen = chooseExactly(
                    2,
                    from = hand,
                    chooser = Chooser.Controller,
                    prompt = "Choose two cards from your hand to manifest"
                )
                val manifested = moveTracked(
                    chosen,
                    CardDestination.ToZone(Zone.BATTLEFIELD),
                    faceDown = FaceDownMode.MANIFEST
                )
                listOf(manifested)
            }
            run(Effects.DrawCards(allManifested.count))
        }
        description = "When you cast this spell, up to two target players each manifest two cards " +
            "from their hands. For each card manifested this way, you draw a card."
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 3,
            toughnessBonus = 2,
            filter = GroupFilter(
                GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless).youControl(),
                excludeSelf = true,
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "10"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04066abb-44d2-4730-9cc3-2584bc4c7d8c.jpg?1783911307"
        ruling(
            "2024-06-07",
            "Kozilek, the Broken Reality's triggered ability will resolve before Kozilek does. If " +
                "Kozilek is countered or otherwise leaves the stack in response to that triggered " +
                "ability, the triggered ability will still resolve as normal."
        )
        ruling(
            "2024-06-07",
            "If a targeted player has only one card in hand as Kozilek's triggered ability " +
                "resolves, they manifest that card."
        )
    }
}
