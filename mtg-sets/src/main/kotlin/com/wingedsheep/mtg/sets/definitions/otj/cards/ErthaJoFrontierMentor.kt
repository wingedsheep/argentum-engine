package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.events.AbilityTargetMatch
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ertha Jo, Frontier Mentor
 * {2}{R}{W}
 * Legendary Creature — Kor Advisor
 * 2/4
 *
 * When Ertha Jo enters, create a 1/1 red Mercenary creature token with "{T}: Target creature you
 * control gets +1/+0 until end of turn. Activate only as a sorcery."
 * Whenever you activate an ability that targets a creature or player, copy that ability. You may
 * choose new targets for the copy.
 *
 * The copy clause reuses [Effects.CopyTargetSpellOrAbility] against
 * [EffectTarget.TriggeringEntity] — for an `AbilityActivatedEvent` the triggering entity is the
 * activated ability already on the stack, and the copy executor prompts for new targets (CR 707.10
 * / 707.10c). Ertha Jo's own triggered ability is not an activated ability, so it doesn't recurse;
 * the Mercenary token's `{T}:` ability does, so activating it copies the pump onto a second creature.
 *
 * The "targets a creature or player" restriction is enforced by
 * [AbilityTargetMatch.CreatureOrPlayer] on the trigger — a non-targeting ability (tap-for-mana,
 * etc.) never fires it.
 */
val ErthaJoFrontierMentor = card("Ertha Jo, Frontier Mentor") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Kor Advisor"
    power = 2
    toughness = 4
    oracleText = "When Ertha Jo enters, create a 1/1 red Mercenary creature token with " +
        "\"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\"\n" +
        "Whenever you activate an ability that targets a creature or player, copy that ability. " +
        "You may choose new targets for the copy."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Mercenary"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = AbilityCost.Tap,
                    effect = Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.CreatureYouControl),
                    timing = TimingRule.SorcerySpeed
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/5/f/5f04607f-eed2-462e-897f-82e41e5f7049.jpg?1712316319"
        )
    }

    triggeredAbility {
        trigger = Triggers.youActivateAbilityTargeting(AbilityTargetMatch.CreatureOrPlayer)
        effect = Effects.CopyTargetSpellOrAbility(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Michal Ivan"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4e81be6-6447-4f1e-be00-6fcdb2ab35af.jpg?1712356089"
    }
}
