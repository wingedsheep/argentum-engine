package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nissa, Steward of Elements — Amonkhet #204 (canonical printing)
 * {X}{G}{U} · Legendary Planeswalker — Nissa · Loyalty X
 *
 * +2: Scry 2.
 * 0: Look at the top card of your library. If it's a land card or a creature card with mana value
 *    less than or equal to the number of loyalty counters on Nissa, you may put that card onto the
 *    battlefield.
 * −6: Untap up to two target lands you control. They become 5/5 Elemental creatures with flying and
 *    haste until end of turn. They're still lands.
 *
 * Loyalty X is `loyaltyIsX`: she enters with as many loyalty counters as the X paid (CR 107.3m),
 * and with none — dying to state-based actions — on any entry that isn't her own cast.
 *
 * The 0 is Explorer's Scope's top-card pipeline with Nissa of Shadowed Boughs' dynamic mana-value
 * cap. The cap reads her loyalty on resolution, falling back to her last-known loyalty if she has
 * left (the ruling); a card not taken stays on top of the library.
 */
val NissaStewardOfElements = card("Nissa, Steward of Elements") {
    manaCost = "{X}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Nissa"
    loyaltyIsX = true
    oracleText = "+2: Scry 2.\n" +
        "0: Look at the top card of your library. If it's a land card or a creature card with mana " +
        "value less than or equal to the number of loyalty counters on Nissa, you may put that card " +
        "onto the battlefield.\n" +
        "−6: Untap up to two target lands you control. They become 5/5 Elemental creatures with " +
        "flying and haste until end of turn. They're still lands."

    // +2: Scry 2.
    loyaltyAbility(+2) {
        effect = Patterns.Library.scry(2)
    }

    // 0: Look at the top card; a land, or a creature with mana value ≤ Nissa's loyalty, may go
    // onto the battlefield.
    loyaltyAbility(0) {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val eligible = filter(
                looked,
                GameObjectFilter.Land or GameObjectFilter.Creature.manaValueAtMostDynamic(
                    DynamicAmounts.countersOnSelf(CounterType.LOYALTY)
                )
            )
            val chosen = chooseUpTo(1, from = eligible, selectedLabel = "Put onto the battlefield")
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    // −6: Untap up to two target lands you control. They become 5/5 Elemental creatures with
    // flying and haste until end of turn. They're still lands.
    loyaltyAbility(-6) {
        targets(TargetFilter(GameObjectFilter.Land.youControl()), count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Untap(EffectTarget.ContextTarget(0)) then
                Effects.BecomeCreature(
                    target = EffectTarget.ContextTarget(0),
                    power = 5,
                    toughness = 5,
                    keywords = setOf(Keyword.FLYING, Keyword.HASTE),
                    creatureTypes = setOf("Elemental"),
                    duration = Duration.EndOfTurn,
                )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "204"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c79036a1-2239-4d4f-8b58-6cf9ac4863fc.jpg?1783936459"

        ruling("2017-04-18", "If X is 0, Nissa enters the battlefield with no loyalty and is put into her owner's graveyard before you can activate her abilities.")
        ruling("2017-04-18", "While resolving Nissa's second ability, if you can't or don't put the card onto the battlefield, it remains on top of your library.")
        ruling("2017-04-18", "Once Nissa's second ability begins to resolve, no player may take actions until it's done. Notably, players can't try to change Nissa's loyalty once you look at the card or choose to put it onto the battlefield.")
        ruling("2017-04-18", "If Nissa leaves the battlefield while her second ability is on the stack, use her last known loyalty to determine whether you may put the top card of your library onto the battlefield. If Nissa left the battlefield because she took enough damage to reduce her loyalty to 0, then her last known loyalty is 0.")
        ruling("2017-04-18", "Nissa's last ability sets the base power and toughness of the lands to 5/5. Any +1/+1 or -1/-1 counters that were on those lands will apply to those values.")
    }
}
