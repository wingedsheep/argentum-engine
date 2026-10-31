package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.conditions.IsInPhase
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yuriko, Blade of the Mighty
 *
 * - "During combat" is the combat phase on anyone's turn (`IsInPhase(COMBAT, yoursOnly = false)`),
 *   and "players" is everyone, Yuriko's controller included.
 * - The cast half is [PlayersCantCastSpells]; the activate half is [PlayersCantActivateAbilities]
 *   with mana abilities exempt and `anyZone` set, since the printed text names no object — it also
 *   stops graveyard and hand abilities such as cycling, and crewing.
 * - "Attacks a player alone" is [AttackPredicate.AloneAgainstPlayer] (CR 506.6): the only creature
 *   declared as attacking that player. Other creatures attacking a planeswalker, a battle or another
 *   player don't stop it; attacking a planeswalker or battle alone doesn't trigger it.
 */
val YurikoBladeOfTheMighty = card("Yuriko, Blade of the Mighty") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Samurai"
    power = 2
    toughness = 3
    oracleText = "During combat, players can't cast spells or activate abilities that aren't mana abilities.\n" +
        "Whenever a creature you control attacks a player alone, it gains double strike until end of turn."

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.Each,
            condition = IsInPhase(listOf(Phase.COMBAT), yoursOnly = false),
        )
    }

    staticAbility {
        ability = PlayersCantActivateAbilities(
            affected = Player.Each,
            condition = IsInPhase(listOf(Phase.COMBAT), yoursOnly = false),
            nonManaAbilitiesOnly = true,
            anyZone = true,
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks(setOf(AttackPredicate.AloneAgainstPlayer))
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.TriggeringEntity, Duration.EndOfTurn)
        description = "Whenever a creature you control attacks a player alone, it gains double strike until " +
            "end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Lie Setiawan"
        flavorText = "\"Emperor Tamiyo rules with truth, respect, and passion. She has more than earned my loyalty.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccbe92a5-42bc-4228-9d5a-212df2f5dc15.jpg?1789128047"
        inBooster = false
    }
}
