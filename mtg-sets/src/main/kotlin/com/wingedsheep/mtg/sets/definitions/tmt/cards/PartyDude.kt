package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Party Dude
 * {G}
 * Enchantment — Class
 *
 * When this Class enters, each player creates a Food token.
 * {1}{G}: Level 2 — Whenever an artifact an opponent controls is put into a graveyard
 *   from the battlefield, draw a card.
 * {4}{G}: Level 3 — Whenever one or more of your opponents are attacked, up to one target
 *   attacking creature gets +X/+X until end of turn, where X is the number of cards in your hand.
 */
val PartyDude = card("Party Dude") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "When this Class enters, each player creates a Food token.\n" +
        "{1}{G}: Level 2\n" +
        "Whenever an artifact an opponent controls is put into a graveyard from the battlefield, draw a card.\n" +
        "{4}{G}: Level 3\n" +
        "Whenever one or more of your opponents are attacked, up to one target attacking creature gets +X/+X until end of turn, where X is the number of cards in your hand."

    // Level 1: each player creates a Food token.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ForEachPlayer(Player.Each, listOf(Effects.CreateFood()))
        description = "When this Class enters, each player creates a Food token."
    }

    classLevel(2, "{1}{G}") {
        triggeredAbility {
            // ANY binding: the artifact leaving is some *other* permanent an opponent controls,
            // not Party Dude itself. The default SELF binding would only fire on Party Dude's own
            // departure, so an opponent's sacrificed Food (an artifact) never triggered the draw.
            trigger = Triggers.leavesBattlefield(
                filter = GameObjectFilter.Artifact.opponentControls(),
                to = Zone.GRAVEYARD,
                binding = TriggerBinding.ANY
            )
            effect = Effects.DrawCards(1)
            description = "Whenever an artifact an opponent controls is put into a graveyard from the battlefield, draw a card."
        }
    }

    classLevel(3, "{4}{G}") {
        triggeredAbility {
            trigger = Triggers.CreaturesAttackYourOpponent
            val pumped = target(
                "up to one target attacking creature",
                TargetCreature(optional = true, filter = TargetFilter(GameObjectFilter.Creature.attacking()))
            )
            effect = Effects.ModifyStats(
                DynamicAmounts.cardsInYourHand(),
                DynamicAmounts.cardsInYourHand(),
                pumped
            )
            description = "Whenever one or more of your opponents are attacked, up to one target attacking creature gets +X/+X until end of turn, where X is the number of cards in your hand."
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Gabriel Rubio"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d27b6f2a-84df-4097-a66a-8e463db47f58.jpg?1777939778"
    }
}
