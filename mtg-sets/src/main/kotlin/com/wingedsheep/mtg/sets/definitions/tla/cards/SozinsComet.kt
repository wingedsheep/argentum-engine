package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sozin's Comet — {3}{R}{R} — Sorcery — Mythic (Avatar: The Last Airbender)
 *
 * Each creature you control gains firebending 5 until end of turn.
 *   (Whenever it attacks, add {R}{R}{R}{R}{R}. This mana lasts until end of combat.)
 * Foretell {2}{R}
 *
 * The mass firebending grant reuses the single-target [Effects.GrantFirebending] (firebending has
 * no engine handler — it's a display keyword plus an attack-triggered combat-duration mana effect)
 * inside a [Effects.ForEachInGroup] over `AllCreaturesYouControl`, granting each of your creatures
 * that exact attack trigger until end of turn: when one attacks this turn it adds {R}{R}{R}{R}{R}
 * (kept through combat).
 *
 * Foretell is the genuine new keyword (CR 702.143): a sorcery-speed special action pays {2} and
 * exiles this card from hand face down, and the card may be cast from exile for its foretell cost
 * {2}{R} on a later turn. See [KeywordAbility.Foretell] plus the engine's ForetellCardHandler /
 * ForetellEnumerator, which reuse Plot's exile-and-recast-from-exile machinery and Airbend's fixed
 * alternative cast cost.
 */
val SozinsComet = card("Sozin's Comet") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Each creature you control gains firebending 5 until end of turn. " +
        "(Whenever it attacks, add {R}{R}{R}{R}{R}. This mana lasts until end of combat.)\n" +
        "Foretell {2}{R} (During your turn, you may pay {2} and exile this card from your hand " +
        "face down. Cast it on a later turn for its foretell cost.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantFirebending(5, EffectTarget.Self)
        )
    }

    keywordAbility(KeywordAbility.foretell("{2}{R}"))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "154"
        artist = "Salvatorre Zee Yazzie"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/649e50e5-299b-4191-87a8-36e9378795be.jpg?1764121059"
    }
}
