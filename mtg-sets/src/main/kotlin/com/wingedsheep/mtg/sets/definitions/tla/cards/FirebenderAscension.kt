package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Firebender Ascension
 * {1}{R}
 * Enchantment
 *
 * When this enchantment enters, create a 2/2 red Soldier creature token with firebending 1.
 * Whenever a creature you control attacking causes a triggered ability of that creature to trigger,
 * put a quest counter on this enchantment. Then if it has four or more quest counters on it, you may
 * copy that ability. You may choose new targets for the copy.
 *
 * Modeling notes (same Ascension cycle as Air/Water/Earthbender Ascension — quest-counter payoff):
 *  - The ETB token is the identical 2/2 red Soldier with firebending 1 that Cruel Administrator
 *    makes: the [firebending] DSL on the inline token def ([firebendingSoldierToken]) grants
 *    [Keyword.FIREBENDING] plus the attack-triggered "add {R} until end of combat" ability, and
 *    both the keyword and that ability are copied onto the created token.
 *  - The payoff trigger is the new [Triggers.AttackCausesYourCreaturesTriggeredAbility]: it fires
 *    when a creature you control's *own* "whenever this creature attacks" ability (a SELF-bound
 *    attacks trigger — e.g. the token's firebending mana ability) is put on the stack. The engine
 *    stamps `causedByAttack` on that `AbilityTriggeredEvent`, so unrelated in-combat triggers
 *    (deals damage, dies) never fire this.
 *  - Putting the quest counter is mandatory; only if the enchantment then has four or more quest
 *    counters does the copy happen — sequenced as `AddCounters` then [ConditionalEffect] gated on
 *    the live count ([Conditions.SourceCounterCountAtLeast]`(QUEST, 4)`), exactly like the sibling
 *    Ascensions. The copy is optional ("you may copy") via [MayEffect] and reuses the shared
 *    [Effects.CopyTargetTriggeredAbility] machinery against [EffectTarget.TriggeringEntity] — the
 *    firebending ability still on the stack beneath this one — which prompts for new targets per
 *    CR 707.10c. Per the printed ruling this ability sits on top of the ability that caused it, so
 *    the triggering ability is guaranteed to still be on the stack when this resolves.
 */
private val firebendingSoldierToken = card("Soldier") {
    typeLine = "Token Creature — Soldier"
    colorIdentity = "R"
    power = 2
    toughness = 2
    firebending(1)
}

val FirebenderAscension = card("Firebender Ascension") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 2/2 red Soldier creature token with firebending 1.\n" +
        "Whenever a creature you control attacking causes a triggered ability of that creature to trigger, " +
        "put a quest counter on this enchantment. Then if it has four or more quest counters on it, you may " +
        "copy that ability. You may choose new targets for the copy."

    // When this enchantment enters, create a 2/2 red Soldier creature token with firebending 1.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.FIREBENDING),
            triggeredAbilities = firebendingSoldierToken.triggeredAbilities,
            imageUri = "https://cards.scryfall.io/normal/front/2/d/2de43b03-9ac5-4292-ab29-2dc6210ef3d9.jpg?1777982247"
        )
        description = "When this enchantment enters, create a 2/2 red Soldier creature token with firebending 1."
    }

    // Whenever a creature you control attacking causes a triggered ability of that creature to
    // trigger, put a quest counter on this enchantment. Then if it has four or more quest counters
    // on it, you may copy that ability. You may choose new targets for the copy.
    triggeredAbility {
        trigger = Triggers.AttackCausesYourCreaturesTriggeredAbility
        effect = Effects.Composite(
            Effects.AddCounters(Counters.QUEST, 1, EffectTarget.Self),
            ConditionalEffect(
                condition = Conditions.SourceCounterCountAtLeast(Counters.QUEST, 4),
                effect = MayEffect(
                    effect = Effects.CopyTargetTriggeredAbility(EffectTarget.TriggeringEntity),
                    descriptionOverride = "Copy that triggered ability? You may choose new targets for the copy."
                )
            )
        )
        description = "Whenever a creature you control attacking causes a triggered ability of that creature to trigger, put a quest counter on this enchantment. Then if it has four or more quest counters on it, you may copy that ability. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Tetsuko"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2929b702-03c6-4cf0-a3f7-61b27f4803be.jpg?1764120940"
    }
}
