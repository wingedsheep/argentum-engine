package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.effects.ManaSpentAsAnyColor
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Draugr Necromancer — Kaldheim #86 (canonical printing).
 * {3}{B} · Snow Creature — Zombie Cleric · 4/4
 *
 * If a nontoken creature an opponent controls would die, exile that card with an ice counter on it
 * instead.
 * You may cast spells from among cards in exile your opponents own with ice counters on them, and
 * you may spend mana from snow sources as though it were mana of any color to cast those spells.
 *
 * - The death shield is Valentin, Dean of the Vein's opponent-creature redirect carrying Dauthi
 *   Voidwalker's counter rider: [RedirectZoneChangeWith] puts the ice counter on the exiled card.
 * - The cast permission is a live [MayPlayCardsFromExile] filter (Tinybones, Bauble Burglar), so it
 *   covers cards iced by an earlier Necromancer too (ruling). It reads "cast spells", so lands are
 *   filtered out — a dead animated land is iced but can't be played (ruling) — and ownership is
 *   checked, so your own card an opponent controlled is iced but not castable (ruling).
 * - [ManaSpentAsAnyColor.SNOW]: mana a snow source produced, of any color or colorless, may pay
 *   those spells' colored pips; other mana still has to match.
 */
val DraugrNecromancer = card("Draugr Necromancer") {
    manaCost = "{3}{B}"
    typeLine = "Snow Creature — Zombie Cleric"
    power = 4
    toughness = 4
    oracleText = "If a nontoken creature an opponent controls would die, exile that card with an ice counter on it instead.\n" +
        "You may cast spells from among cards in exile your opponents own with ice counters on them, and you may " +
        "spend mana from snow sources as though it were mana of any color to cast those spells."

    replacementEffect(
        RedirectZoneChangeWith(
            newDestination = Zone.EXILE,
            additionalEffect = Effects.AddCounters(CounterType.ICE, 1, EffectTarget.TriggeringEntity),
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.nontoken().opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    staticAbility {
        ability = MayPlayCardsFromExile(
            filter = GameObjectFilter.Nonland.ownedByOpponent().withCounter(CounterType.ICE),
            spendAsAnyColor = ManaSpentAsAnyColor.SNOW,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "David Rapoza"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76d8e0ff-e720-41ca-af69-35585a2d7ae2.jpg?1783928251"
        ruling("2021-02-05", "If an opponent controls a nontoken creature that you own and that creature would die, it will be exiled with an ice counter on it. However, you won't be able to cast that card from exile.")
        ruling("2021-02-05", "Similarly, if a nontoken land an opponent controls that's become a creature dies, that card will be exiled with an ice counter, but Draugr Necromancer doesn't allow you to play lands from exile.")
        ruling("2021-02-05", "Casting a spell from exile this way follows the normal rules for casting that spell. You must pay its costs, and you must follow all applicable timing rules.")
        ruling("2021-02-05", "Draugr Necromancer allows you to cast spells from among any cards in exile your opponents own with ice counters on them, including cards that were exiled by previous Draugr Necromancers.")
    }
}
