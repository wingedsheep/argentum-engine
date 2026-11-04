package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val DinosaurSpell = GameObjectFilter.Any.withSubtype(Subtype.DINOSAUR)

/**
 * Cherished Hatchling — Rivals of Ixalan #124
 * {1}{G} · Creature — Dinosaur 2/1
 *
 * When this creature dies, you may cast Dinosaur spells this turn as though they had flash, and
 * whenever you cast a Dinosaur spell this turn, it gains "When this creature enters, you may have
 * it fight another target creature."
 *
 * - The flash half is [Effects.GrantFlashToSpells] for the rest of the turn (Teferi, Time
 *   Raveler's shape); "you may cast" is the permission itself, so the dies trigger isn't optional.
 * - The cast half is Glimpse of Nature's turn-bounded, non-`fireOnce` delayed cast trigger whose
 *   effect is Rivaz of the Claw's grant onto the spell ([EffectTarget.TriggeringEntity],
 *   [Duration.Permanent]) — it lands on the permanent spell on the stack and carries onto the
 *   creature it becomes. It covers every Dinosaur spell cast this turn, flashed or not.
 * - The granted trigger is `optional = true` (lowered to `Effects.May`) over one mandatory target —
 *   the target is announced on the stack, and whether to fight is chosen on resolution.
 */
val CherishedHatchling = card("Cherished Hatchling") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 1
    oracleText = "When this creature dies, you may cast Dinosaur spells this turn as though they had " +
        "flash, and whenever you cast a Dinosaur spell this turn, it gains \"When this creature " +
        "enters, you may have it fight another target creature.\""

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.GrantFlashToSpells(
            spellFilter = DinosaurSpell,
            duration = Duration.EndOfTurn,
        ) then Effects.CreateDelayedTrigger(
            trigger = Triggers.you.casts(DinosaurSpell),
            fireOnce = false,
            expiry = DelayedTriggerExpiry.EndOfTurn,
            effect = Effects.GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.self.enters()
                    val other = target(TargetFilter.OtherCreature)
                    optional = true
                    effect = Effects.Fight(EffectTarget.Self, other)
                    description = "When this creature enters, you may have it fight another target creature."
                },
                target = EffectTarget.TriggeringEntity,
                duration = Duration.Permanent,
            ),
        )
        description = "When this creature dies, you may cast Dinosaur spells this turn as though " +
            "they had flash, and whenever you cast a Dinosaur spell this turn, it gains \"When this " +
            "creature enters, you may have it fight another target creature.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Sam Rowan"
        flavorText = "Despite its appearance, it's anything but defenseless."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a14fe6c-b272-415b-974d-c60d016ab786.jpg?1783935290"

        ruling("2018-01-19", "During the turn Cherished Hatchling dies, you may cast any number of Dinosaurs as though they had flash.")
        ruling("2018-01-19", "You must pay the costs for spells you cast this way. If there's an alternative cost you can pay instead of the mana cost for a Dinosaur spell, you may pay that cost instead.")
        ruling("2018-01-19", "For the triggered ability that the entering Dinosaur gains, if the target is illegal when it tries to resolve or if the Dinosaur that entered the battlefield has left the battlefield, no creature will deal or be dealt damage.")
        ruling("2018-01-19", "For the triggered ability that the entering Dinosaur gains, you choose a target when the ability goes on the stack, but you don't choose until that ability resolves whether those creatures fight.")
        ruling("2018-01-19", "If an effect refers to a \"[subtype] spell\" or \"[subtype] card,\" it refers only to a spell or card that has that subtype. For example, March of the Drowned is a card that benefits Pirates and features Pirates in its illustration, but it isn't a Pirate card.")
    }
}
