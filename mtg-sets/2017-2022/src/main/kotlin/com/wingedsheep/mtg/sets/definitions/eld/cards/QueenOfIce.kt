package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Queen of Ice // Rage of Winter
 * {2}{U}
 * Creature — Human Noble Wizard
 * 2/3
 *
 * Whenever this creature deals combat damage to a creature, tap that creature. It doesn't untap
 * during its controller's next untap step.
 *
 * Adventure: Rage of Winter — {1}{U}, Sorcery — Adventure
 * Tap target creature. It doesn't untap during its controller's next untap step.
 *
 * Both halves use the `DOESNT_UNTAP` grant bounded by
 * [Duration.UntilAfterAffectedControllersNextUntap] — the shape of `chk/cards/KashiTribeWarriors.kt`
 * and `avr/cards/CripplingChill.kt`.
 */
val QueenOfIce = card("Queen of Ice") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Noble Wizard"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature deals combat damage to a creature, tap that creature. " +
        "It doesn't untap during its controller's next untap step."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyCreature)
        effect = Effects.Tap(EffectTarget.TriggeringEntity) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.TriggeringEntity,
                Duration.UntilAfterAffectedControllersNextUntap(),
            )
    }

    adventure("Rage of Winter") {
        manaCost = "{1}{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Tap target creature. It doesn't untap during its controller's next untap step. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Tap(creature) then
                Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    creature,
                    Duration.UntilAfterAffectedControllersNextUntap(),
                )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de2f964a-e4e1-4321-92ad-34b781868e11.jpg?1783932654"
    }
}
