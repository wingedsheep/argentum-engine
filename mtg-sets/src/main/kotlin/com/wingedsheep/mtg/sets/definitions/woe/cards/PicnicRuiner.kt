package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Picnic Ruiner // Stolen Goodies
 * {1}{R}
 * Creature — Goblin Rogue
 * 2/2
 * Whenever this creature attacks while you control a creature with power 4 or greater, this
 * creature gains double strike until end of turn.
 *
 * Adventure: Stolen Goodies — {3}{G}, Sorcery — Adventure
 * Distribute three +1/+1 counters among any number of target creatures you control.
 *
 * "Attacks **while** you control …" is a trigger condition, not an intervening-if: it's checked
 * only as attackers are declared, so per the ruling below the double strike still lands even if the
 * big creature dies in response. `triggerCondition` is exactly that gate — evaluated at detection,
 * not again at resolution.
 *
 * Stolen Goodies is the divide-as-you-choose shape (CR 601.2d): the split is locked in as the spell
 * is cast, and every chosen target must receive at least one counter — so "any number" tops out at
 * three targets, which is what `count = 3` expresses. `minCount = 0` carries the other half of the
 * ruling: the spell is legal with **no** targets at all, and still exiles itself so Picnic Ruiner
 * can be cast from exile later. Counters aimed at a target that has since become illegal are simply
 * lost; the rest of the distribution still applies.
 *
 * The executor splits evenly and hands the remainder to the first chosen target. For a total of
 * three that reproduces *every* legal division — [3] on one target, [2,1] on two (ordered by which
 * was chosen first), [1,1,1] on three — so no explicit allocation step is needed here.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val PicnicRuiner = card("Picnic Ruiner") {
    manaCost = "{1}{R}"
    colorIdentity = "RG"
    typeLine = "Creature — Goblin Rogue"
    oracleText = "Whenever this creature attacks while you control a creature with power 4 or " +
        "greater, this creature gains double strike until end of turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4))
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.Self)
    }

    adventure("Stolen Goodies") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Distribute three +1/+1 counters among any number of target creatures you " +
            "control. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            target(
                "any number of target creatures you control",
                TargetCreature(
                    count = 3,
                    minCount = 0,
                    filter = TargetFilter.CreatureYouControl,
                ),
            )
            effect = Effects.DistributeCountersAmongTargets(totalCounters = 3)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66485c3e-3b21-4db4-ac12-af04e35b49b1.jpg?1783915064"

        ruling(
            "2023-09-01",
            "If you controlled a creature with power 4 or greater when you declared Picnic Ruiner " +
                "as an attacker, it doesn't matter whether you still control one as its ability " +
                "resolves. Picnic Ruiner will still gain double strike until end of turn."
        )
        ruling(
            "2023-09-01",
            "You choose how the counters will be distributed as you cast Stolen Goodies. Each " +
                "target must receive at least one +1/+1 counter."
        )
        ruling(
            "2023-09-01",
            "If some of the creatures are illegal targets as Stolen Goodies tries to resolve, the " +
                "original distribution of counters still applies and the counters that would have " +
                "been put on illegal targets are lost."
        )
        ruling(
            "2023-09-01",
            "You can cast Stolen Goodies with no targets. If you do, you won't distribute any " +
                "counters, but you'll still exile it as it resolves, and you'll still be able to " +
                "cast Picnic Ruiner later."
        )
    }
}
