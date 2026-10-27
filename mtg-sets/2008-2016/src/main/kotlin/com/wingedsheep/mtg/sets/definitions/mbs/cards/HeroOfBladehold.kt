package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hero of Bladehold
 * {2}{W}{W}
 * Creature — Human Knight
 * 3/4
 *
 * Battle cry (Whenever this creature attacks, each other attacking creature gets +1/+0 until end
 * of turn.)
 * Whenever this creature attacks, create two 1/1 white Soldier creature tokens that are tapped and
 * attacking.
 *
 * Battle cry is modeled from its reminder text, as on Sanguine Evangelist: a self-attacks trigger
 * that gives each other attacking creature its own +1/+0. The two attack triggers are separate, so
 * the controller orders them — resolving the token trigger first lets battle cry pump the tokens
 * (2011-06-01 ruling). Each token's defender is chosen by its controller as it enters (CR 508.4).
 */
val HeroOfBladehold = card("Hero of Bladehold") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 4
    oracleText = "Battle cry (Whenever this creature attacks, each other attacking creature gets +1/+0 " +
        "until end of turn.)\n" +
        "Whenever this creature attacks, create two 1/1 white Soldier creature tokens that are tapped and attacking."

    // Battle cry
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            GroupFilter(
                baseFilter = GameObjectFilter.Creature.attacking(),
                excludeSelf = true,
            ),
            Effects.ModifyStats(1, 0, EffectTarget.IterationEntity),
        )
        description = "Battle cry (Whenever this creature attacks, each other attacking creature gets " +
            "+1/+0 until end of turn.)"
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            tapped = true,
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            imageUri = "https://cards.scryfall.io/normal/front/7/a/7a563df7-7b9d-4692-ab1b-578be1887b62.jpg?1783941682",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "8"
        artist = "Austin Hsu"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a3853ec-e307-46e0-96d7-0706b5c45c5e.jpg?1783941393"
        ruling(
            "2011-06-01",
            "Whenever Hero of Bladehold attacks, both abilities will trigger. You can put them onto the stack in any order. If the token-creating ability resolves first, the tokens each get +1/+0 until end of turn from the battle cry ability.",
        )
        ruling(
            "2011-06-01",
            "You choose which opponent or planeswalker an opponent controls that each token is attacking when it is put onto the battlefield.",
        )
        ruling(
            "2011-06-01",
            "Although the tokens are attacking, they never were declared as attacking creatures (for purposes of abilities that trigger whenever a creature attacks, for example).",
        )
    }
}
