package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Caesar, Legion's Emperor — Fallout #1
 * {1}{R}{W}{B} · Legendary Creature — Human Soldier · Mythic · 4/4
 *
 * Whenever you attack, you may sacrifice another creature. When you do, choose two —
 * • Create two 1/1 red and white Soldier creature tokens with haste that are tapped and attacking.
 * • You draw a card and you lose 1 life.
 * • Caesar deals damage equal to the number of creature tokens you control to target opponent.
 *
 * Shape notes:
 *  - "Whenever you attack" is the player-level attack trigger, once per combat however many
 *    creatures attack — [Triggers.you] `attacks()`.
 *  - "You may sacrifice another creature. When you do" is CR 603.12's reflexive shape (Ruthless
 *    Lawbringer): the sacrifice is the optional `action`, chosen at resolution and never a target,
 *    and the modal is the `reflexiveEffect`, whose modes and target are chosen as that second
 *    ability goes on the stack (Scryfall ruling, 2024-03-08).
 *  - The Soldiers say only "tapped and attacking", so CR 508.4 lets their controller choose, as
 *    each enters, the player, planeswalker or battle it attacks:
 *    [EnteringAttackDefender.CONTROLLER_CHOOSES]. They were never declared as attackers, so they
 *    don't trigger "whenever you attack" — Caesar's own ability included.
 *  - Chosen modes resolve in the order written (CR 608.2c), so choosing the tokens and the damage counts the
 *    two new Soldiers.
 */
val CaesarLegionsEmperor = card("Caesar, Legion's Emperor") {
    manaCost = "{1}{R}{W}{B}"
    typeLine = "Legendary Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "Whenever you attack, you may sacrifice another creature. When you do, choose two —\n" +
        "• Create two 1/1 red and white Soldier creature tokens with haste that are tapped and attacking.\n" +
        "• You draw a card and you lose 1 life.\n" +
        "• Caesar deals damage equal to the number of creature tokens you control to target opponent."

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val sacrificed = selectTarget(
                    TargetObject(filter = TargetFilter.CreatureYouControl.other()),
                    nonTargeting = true,
                    prompt = "Choose another creature to sacrifice"
                )
                run(Effects.SacrificeTarget(sacrificed.asTarget))
            },
            optional = true,
            reflexiveEffect = Effects.Modal(
                chooseCount = 2,
                modes = listOf(
                    Mode.noTarget(
                        effect = Effects.CreateToken(
                            power = 1,
                            toughness = 1,
                            colors = setOf(Color.RED, Color.WHITE),
                            creatureTypes = setOf("Soldier"),
                            keywords = setOf(Keyword.HASTE),
                            count = 2,
                            tapped = true,
                            attacking = true,
                            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
                            imageUri = "https://cards.scryfall.io/normal/front/a/a/aae7bdfe-fe14-4a18-b2b0-16e9175a0441.jpg?1783912044",
                        ),
                        description = "Create two 1/1 red and white Soldier creature tokens with haste " +
                            "that are tapped and attacking."
                    ),
                    Mode.noTarget(
                        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller),
                        description = "You draw a card and you lose 1 life."
                    ),
                    mode("Caesar deals damage equal to the number of creature tokens you control to target opponent.") {
                        val opponent = target(Targets.Opponent)
                        effect = Effects.DealDamage(
                            DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.token()),
                            opponent
                        )
                    },
                ),
            ),
            descriptionOverride = "You may sacrifice another creature. When you do, choose two — create two " +
                "1/1 red and white Soldier tokens with haste that are tapped and attacking; you draw a card " +
                "and lose 1 life; or Caesar deals damage equal to the number of creature tokens you control " +
                "to target opponent."
        )
        description = "Whenever you attack, you may sacrifice another creature. When you do, choose two."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Alexander Gering"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/246a7176-98bc-4f54-aa61-38747420f178.jpg?1783912422"
        ruling(
            "2024-03-08",
            "You don't choose modes or a target for Caesar, Legion's Emperor's ability at the time it " +
                "triggers. Rather, a second \"reflexive\" ability triggers when you sacrifice another " +
                "creature this way. You choose modes and, if you chose the third mode, a target for that " +
                "ability as it goes on the stack. Each player may respond to this triggered ability as normal."
        )
        ruling(
            "2024-03-08",
            "The Soldier tokens created by Caesar's ability enter the battlefield tapped and attacking, " +
                "but since they weren't declared as attacking creatures, no abilities that trigger " +
                "whenever a creature attacks or whenever you attack (such as Caesar's own ability) will trigger."
        )
    }
}
