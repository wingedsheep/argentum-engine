package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Driven // Despair — Hour of Devastation #157
 *
 * Driven {1}{G} — Sorcery: Until end of turn, creatures you control gain trample and "Whenever this
 *   creature deals combat damage to a player, draw a card."
 * Despair {1}{B} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Until end of turn, creatures you control gain menace and "Whenever this creature deals combat
 *   damage to a player, that player discards a card."
 *
 * Each half locks its group in at resolution (CR 611.2c): it grants to the creatures you control
 * then, one by one, so creatures you begin to control later in the turn get nothing. "That player"
 * in Despair's granted trigger is the damaged player.
 */
val DrivenDespair = card("Driven // Despair") {
    layout = CardLayout.SPLIT
    colorIdentity = "BG"

    face("Driven") {
        manaCost = "{1}{G}"
        typeLine = "Sorcery"
        oracleText = "Until end of turn, creatures you control gain trample and \"Whenever this creature " +
            "deals combat damage to a player, draw a card.\""

        val drawOnDamage = grantedTriggeredAbility {
            trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
            effect = Effects.DrawCards(1)
            description = "Whenever this creature deals combat damage to a player, draw a card."
        }

        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter.AllCreaturesYouControl,
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity) then
                    Effects.GrantTriggeredAbility(drawOnDamage, EffectTarget.IterationEntity)
            )
        }
    }

    face("Despair") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Until end of turn, creatures you control gain menace and \"Whenever this creature deals " +
            "combat damage to a player, that player discards a card.\""
        keywords(Keyword.AFTERMATH)

        val discardOnDamage = grantedTriggeredAbility {
            trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
            effect = Patterns.Hand.discardCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
            description = "Whenever this creature deals combat damage to a player, that player discards a card."
        }

        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter.AllCreaturesYouControl,
                Effects.GrantKeyword(Keyword.MENACE, EffectTarget.IterationEntity) then
                    Effects.GrantTriggeredAbility(discardOnDamage, EffectTarget.IterationEntity)
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/7713ba59-dd4c-4b49-93a7-292728df86b8.jpg?1783936003"
        ruling(
            "2017-07-14",
            "If you resolve multiples of Driven or of Despair in one turn, your creatures have that many " +
                "instances of the appropriate triggered ability. Each instance triggers separately."
        )
        ruling(
            "2017-07-14",
            "Driven and Despair each affect only creatures you control at the time it resolves. Creatures " +
                "you begin to control later in the turn won't gain the keyword ability or the triggered ability."
        )
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
