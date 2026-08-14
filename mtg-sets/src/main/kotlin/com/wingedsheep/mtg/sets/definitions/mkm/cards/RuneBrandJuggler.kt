package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Rune-Brand Juggler — Murders at Karlov Manor #229
 * {B}{R} · Creature — Human Shaman · 2/2
 *
 * When this creature enters, suspect up to one other target creature you control.
 * {3}{B}{R}, Sacrifice a suspected creature: Target creature gets -5/-5 until end of turn.
 *
 * The two halves are a self-contained engine: the enters trigger manufactures the fuel that the
 * activated ability burns. Suspecting your *own* creature is a genuine cost (CR 701.60a hands it
 * menace *and* "this creature can't block" for as long as it stays suspected), paid up front for
 * removal later.
 *
 * Wording details the script follows literally rather than approximating:
 *
 * - **"up to one other target creature you control"** — optional (a Juggler cast into an empty
 *   board still resolves and simply suspects nothing) and `.other()` (it can't suspect itself,
 *   unlike Person of Interest).
 * - **"Sacrifice a suspected creature"** is a cost, not an effect, so it's paid on activation and
 *   can't be responded to; the filter is `Creature.suspected()`, which reads the live suspected
 *   status rather than "a creature this card suspected". Anyone's suspecting effect — or an
 *   opponent's Convenient Target on your own creature — feeds it, and a creature that has since
 *   been un-suspected no longer qualifies.
 * - **"Target creature"** is unrestricted: -5/-5 can be pointed at your own board, and the
 *   sacrificed suspected creature may itself be the target (it's already gone by resolution, so
 *   the ability fizzles — legal, just pointless).
 *
 * Nothing links the sacrifice to the trigger, so the ability is repeatable across turns as long as
 * you keep producing suspected bodies.
 */
val RuneBrandJuggler = card("Rune-Brand Juggler") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Shaman"
    oracleText = "When this creature enters, suspect up to one other target creature you control. " +
        "(A suspected creature has menace and can't block.)\n" +
        "{3}{B}{R}, Sacrifice a suspected creature: Target creature gets -5/-5 until end of turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target(
            "up to one other target creature you control",
            TargetCreature(
                filter = TargetFilter(GameObjectFilter.Creature.youControl()).other(),
                optional = true
            )
        )
        effect = Effects.Suspect(victim)
        description = "When this creature enters, suspect up to one other target creature you control."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{B}{R}"),
            Costs.Sacrifice(GameObjectFilter.Creature.suspected())
        )
        val victim = target("target creature", TargetCreature())
        effect = Effects.ModifyStats(-5, -5, victim)
        description = "Target creature gets -5/-5 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Mila Pesic"
        flavorText = "Notice: Guests of Hellbender may be exposed to occasional bursts of flame."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5288cf17-9d79-4d35-85f1-bf4d0a73494b.jpg?1783912839"

        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until it " +
                "leaves the battlefield or another effect causes it to no longer be suspected."
        )
    }
}
