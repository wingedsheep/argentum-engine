package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mythos of Illuna — Ikoria: Lair of Behemoths #58
 * {2}{U}{U} · Sorcery
 *
 * Create a token that's a copy of target permanent. If {R}{G} was spent to cast this spell,
 * instead create a token that's a copy of that permanent, except the token has "When this token
 * enters, if it's a creature, it fights up to one target creature you don't control."
 *
 * The "instead" is a mutually exclusive branch on [Conditions.ManaSpentToCastIncludes] (checked
 * as the spell resolves; a copy of the spell had no mana spent). The bonus copy carries the extra
 * ability as part of the copy effect, so it is on the token as it enters and its own
 * enters-trigger sees it. "If it's a creature" is an intervening if (CR 603.4): checked on the
 * projected token as it enters and again on resolution. "Up to one" is an optional target.
 */
val MythosOfIlluna = card("Mythos of Illuna") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "GRU"
    typeLine = "Sorcery"
    oracleText = "Create a token that's a copy of target permanent. If {R}{G} was spent to cast this " +
        "spell, instead create a token that's a copy of that permanent, except the token has \"When " +
        "this token enters, if it's a creature, it fights up to one target creature you don't control.\""

    spell {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.If(
            condition = Conditions.ManaSpentToCastIncludes(requiredRed = 1, requiredGreen = 1),
            then = Effects.CreateTokenCopyOfTarget(
                permanent,
                triggeredAbilities = listOf(
                    grantedTriggeredAbility {
                        trigger = Triggers.self.enters()
                        interveningIf = Conditions.SourceMatches(GameObjectFilter.Creature)
                        val opposingCreature = target(TargetFilter.CreatureOpponentControls, optional = true)
                        effect = Effects.Fight(EffectTarget.Self, opposingCreature)
                        description = "When this token enters, if it's a creature, it fights up to " +
                            "one target creature you don't control."
                    }
                ),
            ),
            otherwise = Effects.CreateTokenCopyOfTarget(permanent),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1b07082-c5a5-4283-a2f5-5b1e0120d752.jpg?1783931072"
        ruling(
            "2020-04-17",
            "If {R}{G} was spent to cast the spell, the token has the extra ability. If something copies " +
                "the token later, the copy will also have that ability."
        )
        ruling(
            "2020-04-17",
            "If the token isn't a creature immediately after it enters the battlefield, the granted ability " +
                "won't trigger. You can't take actions to make that token into a creature after it has " +
                "entered the battlefield."
        )
        ruling(
            "2020-04-17",
            "If the target creature is an illegal target or if the token is no longer a creature when the " +
                "token's granted ability tries to resolve, the ability doesn't resolve. If it's a legal " +
                "target but the token is no longer on the battlefield when the ability resolves, the target " +
                "creature won't deal or be dealt damage."
        )
        ruling(
            "2020-04-17",
            "If an effect copies the Mythos spell, no mana was spent to cast the copy, so the copy won't " +
                "receive the bonus."
        )
    }
}
