package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Snooping Page
 * {1}{W}{B}
 * Creature — Human Cleric
 * 2/3
 * Repartee — Whenever you cast an instant or sorcery spell that targets a creature, this creature
 * can't be blocked this turn.
 * Whenever this creature deals combat damage to a player, you draw a card and lose 1 life.
 *
 * "Repartee" is an ability word (flavor only). The first trigger is the standard
 * `youCastSpell(InstantOrSorcery)` narrowed by `targetsMatching(Creature)` shared with the other
 * SOS Repartee cards; it grants SELF the floating `CANT_BE_BLOCKED` flag for the turn. The second
 * is a plain combat-damage-to-player trigger that draws and self-pays 1 life.
 */
val SnoopingPage = card("Snooping Page") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "Repartee — Whenever you cast an instant or sorcery spell that targets a " +
        "creature, this creature can't be blocked this turn.\n" +
        "Whenever this creature deals combat damage to a player, you draw a card and lose 1 life."

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.InstantOrSorcery.targetsMatching(GameObjectFilter.Creature)
        )
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self, Duration.EndOfTurn)
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(1, EffectTarget.Controller),
                Effects.LoseLife(1, EffectTarget.Controller),
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Alexandre Honoré"
        flavorText = "Eavesdropping is not a misdeed in the Forum, but a fact of life."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73d06987-8686-461b-b260-9a4fee6a3b32.jpg?1775938584"
    }
}
