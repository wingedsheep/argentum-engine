package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Naga Fleshcrafter
 * {3}{U}
 * Creature — Snake Shapeshifter
 * 0/0
 *
 * You may have this creature enter as a copy of any creature on the battlefield.
 * Renew — {2}{U}, Exile this card from your graveyard: Put a +1/+1 counter on target
 *   nonlegendary creature you control. Each other creature you control becomes a copy of
 *   that creature until end of turn. Activate only as a sorcery.
 *
 * The enter-as-copy ability is the standard Clone replacement [EntersAsCopy] (optional). The
 * renew ability composes two atoms over a single target: [Effects.AddCounters] puts the +1/+1
 * counter on the target, then [Effects.EachPermanentBecomesCopyOfTarget] makes every **other**
 * creature you control (`excludeTarget = true`) a copy of that target **until end of turn**
 * (`duration = Duration.EndOfTurn`, reverted by the cleanup step). Per the rulings the copies
 * take copiable values only and no "enters" abilities fire (nothing enters).
 */
val NagaFleshcrafter = card("Naga Fleshcrafter") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of any creature on the battlefield.\n" +
        "Renew — {2}{U}, Exile this card from your graveyard: Put a +1/+1 counter on target " +
        "nonlegendary creature you control. Each other creature you control becomes a copy of " +
        "that creature until end of turn. Activate only as a sorcery."

    replacementEffect(EntersAsCopy(optional = true))

    renew("{2}{U}") {
        val creature = target(
            "nonlegendary creature you control",
            TargetCreature(
                filter = TargetFilter(GameObjectFilter.Creature.youControl().nonlegendary())
            )
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
            .then(
                Effects.EachPermanentBecomesCopyOfTarget(
                    target = creature,
                    filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                    duration = Duration.EndOfTurn,
                    excludeTarget = true,
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5df17423-9fdd-4432-8660-1d267c685595.jpg?1743204171"
    }
}
