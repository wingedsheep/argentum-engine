package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emrakul, the World Anew
 * {12}
 * Legendary Creature — Eldrazi
 * 12/12
 *
 * - "Protection from spells and from permanents that were cast this turn" is two protection
 *   abilities (CR 702.16g) over the source-kind scopes [ProtectionScope.Spells] and
 *   [ProtectionScope.PermanentsCastThisTurn]. A creature that was cast this turn can't block it,
 *   and its damage to Emrakul is prevented; a spell can't target it or damage it.
 * - The cast trigger resolves before Emrakul and still resolves if Emrakul is countered; the
 *   control change is permanent and outlives Emrakul (rulings).
 */
val EmrakulTheWorldAnew = card("Emrakul, the World Anew") {
    manaCost = "{12}"
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi"
    power = 12
    toughness = 12
    oracleText = "When you cast this spell, gain control of all creatures target player controls.\n" +
        "Flying, protection from spells and from permanents that were cast this turn\n" +
        "When Emrakul leaves the battlefield, sacrifice all creatures you control.\n" +
        "Madness—Pay six {C}."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val player = target(Targets.Player)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player)),
            Effects.GainControl(EffectTarget.IterationEntity, Duration.Permanent)
        )
        description = "When you cast this spell, gain control of all creatures target player controls."
    }

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Spells))
    keywordAbility(KeywordAbility.Protection(ProtectionScope.PermanentsCastThisTurn))

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.SacrificeAll(GameObjectFilter.Creature.youControl())
        description = "When Emrakul leaves the battlefield, sacrifice all creatures you control."
    }

    keywordAbility(KeywordAbility.madness("{C}{C}{C}{C}{C}{C}"))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b914bfd9-5be3-4459-a68a-9b3ea2747bbc.jpg?1783911308"
        ruling(
            "2024-06-07",
            "Emrakul, the World Anew's triggered ability will resolve before Emrakul does. If Emrakul " +
                "is countered or otherwise leaves the stack in response to that triggered ability, the " +
                "triggered ability will still resolve as normal."
        )
        ruling(
            "2024-06-07",
            "The control-change effect of Emrakul's first ability lasts indefinitely. It doesn't wear " +
                "off during the cleanup step or when Emrakul leaves the battlefield."
        )
    }
}
