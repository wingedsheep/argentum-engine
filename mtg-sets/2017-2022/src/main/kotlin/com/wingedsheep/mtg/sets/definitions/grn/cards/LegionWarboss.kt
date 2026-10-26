package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Legion Warboss
 * {2}{R}
 * Creature — Goblin Soldier
 * 2/2
 *
 * Mentor
 * At the beginning of combat on your turn, create a 1/1 red Goblin creature token. That token gains
 * haste until end of turn and attacks this combat if able.
 *
 * The created token is addressed through the pipeline's [CREATED_TOKENS] slot (Howlsquad Heavy's
 * shape), so the haste grant and the attack requirement land on exactly that token.
 */
val LegionWarboss = card("Legion Warboss") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Soldier"
    power = 2
    toughness = 2
    oracleText = "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking " +
        "creature with lesser power.)\n" +
        "At the beginning of combat on your turn, create a 1/1 red Goblin creature token. That token " +
        "gains haste until end of turn and attacks this combat if able."

    mentor()

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/b/2/b27dc0bd-ba46-4a5c-9559-7c11eb4ec37b.jpg?1783934062",
        ) then
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.PipelineTarget(CREATED_TOKENS, 0)) then
            Effects.MarkMustAttackNextCombat(EffectTarget.PipelineTarget(CREATED_TOKENS, 0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Alex Konstad"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e84cb9c-9876-47a4-aea4-78574321bc36.jpg?1783934160"
        ruling(
            "2018-10-05",
            "If the token can't attack for any reason (such as becoming tapped), then it doesn't attack. " +
                "If there's a cost associated with having it attack, its controller isn't forced to pay " +
                "that cost, so it doesn't have to attack in that case either."
        )
        ruling(
            "2018-10-05",
            "Mentor compares the power of the creature with mentor with that of the target creature at " +
                "two different times: once as the triggered ability is put onto the stack, and once as the " +
                "triggered ability resolves. If you wish to raise a creature's power so its mentor ability " +
                "can target a bigger creature, the last chance you have to do so is during the beginning " +
                "of combat step."
        )
        ruling(
            "2018-10-05",
            "If the creature with mentor leaves the battlefield with mentor on the stack, use its power " +
                "as that creature last existed on the battlefield to determine whether the target creature " +
                "has less power."
        )
    }
}
