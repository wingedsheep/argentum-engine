package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tolsimir, Friend to Wolves — War of the Spark #224
 * {2}{G}{G}{W} · Legendary Creature — Elf Scout · 3/3
 *
 * When Tolsimir enters, create Voja, Friend to Elves, a legendary 3/3 green and white Wolf
 * creature token.
 * Whenever a Wolf you control enters, you gain 3 life and that creature fights up to one target
 * creature you don't control.
 *
 * Voja is named apart from its Wolf type, so the token gets `name` and `creatureTypes` separately,
 * plus `legendary = true`. Voja entering triggers the second ability like any other Wolf.
 *
 * The fight pairs the entering Wolf ([EffectTarget.TriggeringEntity]) with an optional target
 * (Struggle for Skemfar's "up to one target creature you don't control" shape). Per the rulings:
 * no target chosen → just the life; a chosen target that's illegal on resolution → the ability
 * doesn't resolve, so no life either; the Wolf gone by then → the life but no fight.
 */
val TolsimirFriendToWolves = card("Tolsimir, Friend to Wolves") {
    manaCost = "{2}{G}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elf Scout"
    power = 3
    toughness = 3
    oracleText = "When Tolsimir enters, create Voja, Friend to Elves, a legendary 3/3 green and " +
        "white Wolf creature token.\n" +
        "Whenever a Wolf you control enters, you gain 3 life and that creature fights up to one " +
        "target creature you don't control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf(Subtype.WOLF.value),
            name = "Voja, Friend to Elves",
            legendary = true,
            imageUri = "https://cards.scryfall.io/normal/front/3/9/39475e91-1e71-4546-ae4a-47ea66e47536.jpg?1783933348",
        )
        description = "When Tolsimir enters, create Voja, Friend to Elves, a legendary 3/3 green " +
            "and white Wolf creature token."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.WOLF).youControl()).enters()
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            ),
            optional = true
        )
        effect = Effects.GainLife(3) then Effects.Fight(EffectTarget.TriggeringEntity, theirs)
        description = "Whenever a Wolf you control enters, you gain 3 life and that creature fights " +
            "up to one target creature you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7fce047-49f1-40ae-8410-6501cb0f8201.jpg?1783933384"
        ruling(
            "2019-05-03",
            "If Tolsimir is still on the battlefield when its enters-the-battlefield trigger " +
                "resolves, its last ability will trigger as Voja is created."
        )
        ruling(
            "2019-05-03",
            "You may put Tolsimir's last ability onto the stack without choosing any target. You'll " +
                "just gain 3 life. However, if you do choose a target and the target permanent is an " +
                "illegal target by the time Tolsimir's last ability tries to resolve, the ability " +
                "doesn't resolve. You don't gain 3 life and no creature deals or is dealt damage."
        )
        ruling(
            "2019-05-03",
            "If the Wolf that entered the battlefield is no longer on the battlefield as Tolsimir's " +
                "last ability resolves, no creature deals or is dealt damage, but you do gain 3 life."
        )
    }
}
