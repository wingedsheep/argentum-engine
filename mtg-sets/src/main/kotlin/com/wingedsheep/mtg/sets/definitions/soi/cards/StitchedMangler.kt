package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stitched Mangler
 * {2}{U}
 * Creature — Zombie Horror
 * 2/3
 *
 * This creature enters tapped.
 * When this creature enters, tap target creature an opponent controls. That creature doesn't
 * untap during its controller's next untap step.
 *
 * "Enters tapped" is the self-replacement [EntersTapped]. The tap + skip-next-untap is the same
 * construction as Crippling Chill: [Effects.Tap] followed by the [AbilityFlag.DOESNT_UNTAP]
 * keyword grant with [Duration.UntilAfterAffectedControllersNextUntap].
 */
val StitchedMangler = card("Stitched Mangler") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    power = 2
    toughness = 3
    oracleText = "This creature enters tapped.\n" +
        "When this creature enters, tap target creature an opponent controls. That creature doesn't " +
        "untap during its controller's next untap step."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            GrantKeywordEffect(
                AbilityFlag.DOESNT_UNTAP.name,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c03c643c-8e4c-4dc9-8756-c253fa057729.jpg?1783937785"
    }
}
