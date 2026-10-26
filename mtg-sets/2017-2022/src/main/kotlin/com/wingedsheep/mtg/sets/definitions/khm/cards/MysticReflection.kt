package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mystic Reflection
 * {1}{U}
 * Instant
 * Choose target nonlegendary creature. The next time one or more creatures or planeswalkers enter
 * this turn, they enter as copies of the chosen creature.
 * Foretell {U}
 *
 * The rider is a one-shot copy replacement the resolving spell grants its caster: an [EntersAsCopy]
 * whose `copyOf` is the target, granted for [Duration.NextUse]. It modifies the first entry event
 * that brings in a creature or planeswalker — under any player's control, every such object entering
 * in that event — and is then spent; unused, it lapses at end of turn. The entrants copy the chosen
 * creature's copiable values as they enter, or as it last existed if it has left the battlefield.
 */
val MysticReflection = card("Mystic Reflection") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose target nonlegendary creature. The next time one or more creatures or " +
        "planeswalkers enter this turn, they enter as copies of the chosen creature.\n" +
        "Foretell {U} (During your turn, you may pay {2} and exile this card from your hand face " +
        "down. Cast it on a later turn for its foretell cost.)"

    spell {
        val chosen = target(TargetFilter.Creature.nonlegendary())
        effect = Effects.GrantReplacementEffect(
            EntersAsCopy(
                optional = false,
                copyOf = chosen,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.CreatureOrPlaneswalker,
                    to = Zone.BATTLEFIELD
                )
            ),
            EffectTarget.Self,
            Duration.NextUse("one or more creatures or planeswalkers enter")
        )
    }

    keywordAbility(KeywordAbility.foretell("{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "YW Tang"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7b877e2-60eb-46cd-acd7-8555b9e7e993.jpg?1783928260"
    }
}
