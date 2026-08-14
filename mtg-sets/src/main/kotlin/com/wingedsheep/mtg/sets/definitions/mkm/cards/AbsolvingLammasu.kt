package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Absolving Lammasu — Murders at Karlov Manor #2
 * {4}{W} · Creature — Lammasu · 4/3
 *
 * Flying
 * When this creature enters, all suspected creatures are no longer suspected.
 * When this creature dies, you gain 3 life and suspect up to one target creature an opponent
 * controls.
 *
 * White's answer to a board full of suspects, and then a parting shot that creates a fresh one. The
 * two halves pull in opposite directions on purpose: the Lammasu clears the table on arrival and
 * hands the designation back to an opponent's creature when it dies.
 *
 * "All suspected creatures" is every creature on the battlefield, not just yours — the ETB is
 * symmetric and will absolve an opponent's suspected attacker just as readily as it frees your own
 * blocker. Modelled as [Effects.ForEachInGroup] over the un-scoped `suspected()` filter, which reads
 * the designation from projected state.
 *
 * [Effects.NoLongerSuspected] (CR 701.60c) strips the whole suspect application — status, menace,
 * and can't-block together — because those two grants exist only for as long as the creature is
 * suspected. Menace a creature has from anywhere else is untouched.
 *
 * The dies trigger is *one* ability with an optional target, so the life gain rides on the same
 * resolution: per the printed ruling, choosing a target and having it become illegal fizzles the
 * whole ability and you gain nothing. Choosing **no** target (the "up to one" line) still gains the
 * 3 life. That's [TargetCreature] with `optional = true`, not two separate abilities.
 *
 * The dies trigger fires from the graveyard on last-known information — by the time it resolves the
 * Lammasu is long gone, which is why nothing in the effect refers back to it.
 */
val AbsolvingLammasu = card("Absolving Lammasu") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Lammasu"
    power = 4
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, all suspected creatures are no longer suspected.\n" +
        "When this creature dies, you gain 3 life and suspect up to one target creature an " +
        "opponent controls. (A suspected creature has menace and can't block.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.suspected()),
            Effects.NoLongerSuspected(EffectTarget.Self)
        )
        description = "When this creature enters, all suspected creatures are no longer suspected."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        val suspect = target(
            "up to one target creature an opponent controls",
            TargetCreature(optional = true, filter = TargetFilter.CreatureOpponentControls)
        )
        effect = Effects.Composite(
            Effects.GainLife(3),
            Effects.Suspect(suspect)
        )
        description = "When this creature dies, you gain 3 life and suspect up to one target " +
            "creature an opponent controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd6e71a1-713e-4eca-bd65-9f0638c16794.jpg?1783912931"

        ruling(
            "2024-02-02",
            "You don't have to choose a target for Absolving Lammasu's last ability. However, if " +
                "you do, and that permanent is an illegal target at the time the ability tries to " +
                "resolve, it won't resolve and none of its effects will happen. You won't gain life."
        )
        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until it " +
                "leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling(
            "2024-02-02",
            "There's no limit to the number of creatures that can be suspected simultaneously. " +
                "Suspecting a new creature doesn't cause other creatures to stop being suspected."
        )
    }
}
