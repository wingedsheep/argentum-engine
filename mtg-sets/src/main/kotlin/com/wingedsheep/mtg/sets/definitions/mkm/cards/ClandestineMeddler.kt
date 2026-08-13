package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Clandestine Meddler — Murders at Karlov Manor #82
 * {2}{B} · Creature — Vampire Rogue · 3/2
 *
 * When this creature enters, suspect up to one other target creature you control.
 * Whenever one or more suspected creatures you control attack, surveil 1.
 *
 * The two halves are a deliberate engine: the ETB manufactures the suspect the attack trigger needs,
 * so a Meddler cast into an empty board still turns on once anything else arrives and gets suspected.
 *
 * "Up to one **other**" is [TargetFilter.OtherCreatureYouControl] — `excludeSelf = true` — so the
 * Meddler can't suspect itself. That matters: suspecting is a real cost (CR 701.60a grants menace
 * *and* "can't block"), and the card is written to spend it on a creature you choose, not on its own
 * body. `optional = true` carries the "up to one": declining is legal even with legal targets
 * available, which is the right play when nothing on your board wants to stop blocking.
 *
 * The attack trigger is [Triggers.YouAttackWithFilter] over `Creature.suspected()`, which fires once
 * per combat no matter how many suspected creatures attack — "one or more" is a batch trigger, not a
 * per-attacker one. The filter carries no `youControl()`: attackers in a `YouAttackEvent` are by
 * definition controlled by the attacking player, matching how AnimPakal and PersistentMarshstalker
 * model the same shape.
 *
 * Nothing here refers back to the Meddler, so it need not be attacking — or even still on the
 * battlefield — for the surveil to happen.
 */
val ClandestineMeddler = card("Clandestine Meddler") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, suspect up to one other target creature you control. " +
        "(A suspected creature has menace and can't block.)\n" +
        "Whenever one or more suspected creatures you control attack, surveil 1. (Look at the top " +
        "card of your library. You may put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val accomplice = target(
            "up to one other target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.OtherCreatureYouControl)
        )
        effect = Effects.Suspect(accomplice)
        description = "When this creature enters, suspect up to one other target creature you control."
    }

    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(GameObjectFilter.Creature.suspected())
        effect = Effects.Surveil(1)
        description = "Whenever one or more suspected creatures you control attack, surveil 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Jodie Muir"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e069de0-3218-456c-b191-93e755634783.jpg?1783912902"

        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until it " +
                "leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling(
            "2024-02-02",
            "Clandestine Meddler's last ability triggers only once each combat, no matter how many " +
                "suspected creatures you attack with."
        )
    }
}
