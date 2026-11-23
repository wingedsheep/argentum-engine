package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.scripting.effects.CopySourceRead
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dimir Doppelganger — Ravnica: City of Guilds #202 (canonical printing)
 * {1}{U}{B} · Creature — Shapeshifter · 0/2
 *
 * {1}{U}{B}: Exile target creature card from a graveyard. This creature becomes a copy of that
 * card, except it has this ability.
 *
 * The Likeness Looter shape with an exile step in front of it. `sourceRead = ANY_ZONE` is what makes the
 * ordering work: the copy reads the card's copiable characteristics (CR 707.2) after it has already
 * left the graveyard for exile, and the target handle still names the same entity on the other side
 * of that zone change.
 *
 * `retainActivatingAbility` is the "except it has this ability" clause, and it is the whole card
 * rather than a rider — the copy replaces the permanent's `CardComponent` wholesale, so without the
 * re-grant the Doppelganger could copy exactly once and then never again. The grant is durable and
 * keyed by entity, so it survives each subsequent copy; the 2005-10-01 ruling ("if it becomes a copy
 * of a different creature card, the new copy will overwrite the old copy") is that same property
 * read from the other side.
 *
 * Any graveyard is a legal source — the text says "a graveyard", not "your graveyard".
 */
val DimirDoppelganger = card("Dimir Doppelganger") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 2
    oracleText = "{1}{U}{B}: Exile target creature card from a graveyard. This creature becomes a " +
        "copy of that card, except it has this ability."

    activatedAbility {
        cost = Costs.Mana("{1}{U}{B}")
        val creatureCard = target(TargetFilter(GameObjectFilter.Creature, zone = Zone.GRAVEYARD))
        effect = Effects.Exile(creatureCard, fromZone = Zone.GRAVEYARD) then
            Effects.EachPermanentBecomesCopyOfTarget(
                target = creatureCard,
                affected = EffectTarget.Self,
                sourceRead = CopySourceRead.ANY_ZONE,
                duration = Duration.Permanent,
                retainActivatingAbility = true,
            )
        description = "{1}{U}{B}: Exile target creature card from a graveyard. This creature " +
            "becomes a copy of that card, except it has this ability."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Jim Murray"
        flavorText = "\"Fear not. Your life will not go unlived.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a48be841-d35c-4d99-aebc-3684b36760ac.jpg?1783943622"
        ruling(
            "2005-10-01",
            "This creature becomes an exact copy of a copied card, except that it also has Dimir " +
                "Doppelganger's activated ability. If it becomes a copy of a different creature " +
                "card, the new copy will overwrite the old copy."
        )
        ruling(
            "2005-10-01",
            "A permanent's ability that refers to cards the creature exiled (such as Sisters of " +
                "Stone Death's third ability) only affects cards exiled by other abilities " +
                "intrinsic to that permanent (such as Sisters of Stone Death's second ability). " +
                "Suppose that (a) Dimir Doppelganger copies Arc-Slogger, (b) its \"deal 2 damage\" " +
                "ability is activated, and then (c) it copies Sisters of Stone Death. Creatures " +
                "exiled by Arc-Slogger's ability can't be returned with Sisters of Stone Death's " +
                "ability."
        )
    }
}
