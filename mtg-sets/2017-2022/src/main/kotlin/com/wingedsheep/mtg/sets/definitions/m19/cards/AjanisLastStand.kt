package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ZoneChangeCause
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val avatarToken = Effects.CreateToken(
    power = 4,
    toughness = 4,
    colors = setOf(Color.WHITE),
    creatureTypes = setOf("Avatar"),
    keywords = setOf(Keyword.FLYING),
    imageUri = "https://cards.scryfall.io/normal/front/5/f/5f3f8f97-0c27-49d6-ae9a-a0fe2a7ce38b.jpg?1783934471",
)

/**
 * Ajani's Last Stand (M19 #4)
 * {2}{W}{W} · Enchantment
 *
 * - The dies trigger is a "you may sacrifice this. If you do" gate ([Effects.IfYouDo] with
 *   [SuccessCriterion.PermanentsSacrificed]): when several creatures/planeswalkers die at once it
 *   triggers that many times, but only the first resolution can sacrifice it, so you get one token
 *   (ruling 2018-07-13).
 * - The discard trigger is the self-bound discard trigger narrowed to an opponent's spell or
 *   ability ([ZoneChangeCause.DiscardedByOpponentEffect]) — cleanup and cost discards never fire
 *   it — with "if you control a Plains" as a CR 603.4 intervening "if", checked both as it
 *   triggers and as it resolves.
 */
val AjanisLastStand = card("Ajani's Last Stand") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature or planeswalker you control dies, you may sacrifice this enchantment. " +
        "If you do, create a 4/4 white Avatar creature token with flying.\n" +
        "When a spell or ability an opponent controls causes you to discard this card, if you control a " +
        "Plains, create a 4/4 white Avatar creature token with flying."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.CreatureOrPlaneswalker.youControl()).dies()
        optional = true
        effect = Effects.IfYouDo(
            action = Effects.SacrificeTarget(EffectTarget.Self),
            then = avatarToken,
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
        description = "Whenever a creature or planeswalker you control dies, you may sacrifice this " +
            "enchantment. If you do, create a 4/4 white Avatar creature token with flying."
    }

    triggeredAbility {
        trigger = Triggers.self.isDiscarded(cause = ZoneChangeCause.DiscardedByOpponentEffect)
        interveningIf = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Plains"))
        effect = avatarToken
        description = "When a spell or ability an opponent controls causes you to discard this card, " +
            "if you control a Plains, create a 4/4 white Avatar creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1aebbb57-31b3-4289-815e-4f529e29f3ea.jpg?1783934610"
        ruling(
            "2018-07-13",
            "If multiple creatures and/or planeswalkers you control die at the same time, the first " +
                "ability of Ajani's Last Stand triggers that many times. However, you can sacrifice it " +
                "only once, so you'll get only one token."
        )
        ruling(
            "2018-07-13",
            "If a spell or ability an opponent controls causes you to make a choice and you choose to " +
                "discard Ajani's Last Stand, its last ability will trigger."
        )
    }
}
