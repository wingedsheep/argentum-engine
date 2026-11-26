package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.LifeLossFloor
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elderscale Wurm
 * {4}{G}{G}{G}
 * Creature — Wurm
 * 7/7
 *
 * Trample
 * When this creature enters, if your life total is less than 7, your life total becomes 7.
 * As long as you have 7 or more life, damage that would reduce your life total to less than 7
 * reduces it to 7 instead.
 *
 * The last ability is a damage-only life-loss floor (like Ali from Cairo): the damage is still
 * dealt in full (lifelink sees it), only the reduction of your life total is capped, and life
 * loss or payment is unaffected. "As long as you have 7 or more life" is checked against the
 * life total before the damage, so at 6 or less damage reduces it as normal.
 */
val ElderscaleWurm = card("Elderscale Wurm") {
    manaCost = "{4}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 7
    toughness = 7
    oracleText = "Trample\n" +
        "When this creature enters, if your life total is less than 7, your life total becomes 7.\n" +
        "As long as you have 7 or more life, damage that would reduce your life total to less than 7 reduces it to 7 instead."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.LifeAtMost(6)
        effect = Effects.SetLifeTotal(7, EffectTarget.Controller)
        description = "If your life total is less than 7, your life total becomes 7."
    }

    replacementEffect(
        LifeLossFloor(
            floor = 7,
            restrictions = listOf(Conditions.LifeAtLeast(7)),
            appliesTo = EventPattern.LifeLossEvent(player = Player.You),
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "167"
        artist = "Richard Wright"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20f3f63d-0f04-4945-9895-940c916a2547.jpg?1783940473"

        ruling("2012-07-01", "Elderscale Wurm's last ability doesn't prevent damage. It only changes the result of damage dealt to you. For example, a creature with lifelink that deals damage to you will still cause its controller to gain life, even if that damage would reduce your life total to less than 7.")
        ruling("2012-07-01", "Elderscale Wurm's last ability doesn't affect the loss or payment of life.")
        ruling("2012-07-01", "If you have less than 7 life, damage dealt to you reduces your life total as normal.")
        ruling("2012-07-01", "If damage is dealt to you and Elderscale Wurm at the same time, and that damage is enough to destroy Elderscale Wurm and reduce your life total to 7 or less, Elderscale Wurm's ability will still apply. For example, if you have 10 life and a spell deals 10 damage to you and 10 damage to Elderscale Wurm, you'll end up at 7 life and Elderscale Wurm will be destroyed.")
        ruling("2012-07-01", "In a Commander game, combat damage you're dealt by a commander is still tracked, even if it doesn't change your life total.")
    }
}
