package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Diminisher Witch
 * {2}{U}
 * Creature — Human Warlock
 * 3/2
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * When this creature enters, if it was bargained, create a Cursed Role token attached to target
 * creature an opponent controls. (If you control another Role on it, put that one into the
 * graveyard. Enchanted creature is 1/1.)
 *
 * The permanent-spell shape of bargain: the "if it was bargained" clause is an intervening-'if'
 * (CR 603.4) on [Conditions.WasBargained], so an unbargained cast never puts the ability on the
 * stack at all — and therefore never asks for a target. Same wiring as Agatha's Champion and
 * Troublemaker Ouphe.
 *
 * The Cursed Role sets base P/T to 1/1 ([Effects.CreateRoleToken] carries the Role's statics and the
 * one-Role-per-creature state-based action, CR 704.5s), which is why the target is an *opponent's*
 * creature here rather than Spiteful Hexmage's own.
 */
val DiminisherWitch = card("Diminisher Witch") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Warlock"
    power = 3
    toughness = 2
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "When this creature enters, if it was bargained, create a Cursed Role token attached to " +
        "target creature an opponent controls. (If you control another Role on it, put that one " +
        "into the graveyard. Enchanted creature is 1/1.)"

    bargain()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasBargained
        val cursed = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.CreateRoleToken("Cursed Role", cursed)
        description = "When this creature enters, if it was bargained, create a Cursed Role token " +
            "attached to target creature an opponent controls."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Fariba Khamseh"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/646d604f-b187-4122-bd4b-67634654b6f1.jpg?1783915121"

        ruling(
            "2023-09-01",
            "Roles are colorless enchantment tokens. Each one has the Aura and Role subtypes and " +
                "the enchant creature ability."
        )
        ruling(
            "2023-09-01",
            "If a permanent has more than one Role attached to it controlled by the same player, " +
                "each of those Roles except the one with the most recent timestamp is put into its " +
                "owner's graveyard. This is a state-based action."
        )
        ruling(
            "2023-09-01",
            "You can bargain a permanent spell even if you won't be able to choose targets for an " +
                "enters-the-battlefield ability of that permanent once the spell resolves."
        )
    }
}
