package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Torrent Sculptor // Flamethrower Sonata — Strixhaven: School of Mages #159 (canonical printing)
 *
 * Modal double-faced card (CR 712). Cast either face from hand, never both.
 *
 * Front — Torrent Sculptor ({2}{U}{U}, Creature — Merfolk Wizard, 2/2):
 *   "Ward {2}"
 *   "When this creature enters, exile an instant or sorcery card from your graveyard. Put a number
 *    of +1/+1 counters on this creature equal to half that card's mana value, rounded up."
 *   The card is chosen on resolution and is mandatory if able (ruling) — a `chooseExactly(1)`
 *   over the graveyard's instants and sorceries, then the counter count reads the exiled card's
 *   mana value off the pipeline collection, halved and rounded up. An empty graveyard exiles
 *   nothing and adds zero counters.
 *
 * Back — Flamethrower Sonata ({1}{R}, Sorcery):
 *   "Discard a card, then draw a card. When you discard an instant or sorcery card this way,
 *    Flamethrower Sonata deals damage equal to that card's mana value to target creature or
 *    planeswalker you don't control."
 *   Glacial Dragonhunt's pipeline (discard, draw, then a targeted reflexive trigger gated on what
 *   was discarded, CR 603.12) with Hidetsugu's mana-value-of-the-collection damage amount. With
 *   an empty hand nothing is discarded, a card is still drawn, and no damage is dealt (ruling).
 */
val TorrentSculptor = card("Torrent Sculptor") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "UR"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent " +
        "controls, counter it unless that player pays {2}.)\n" +
        "When this creature enters, exile an instant or sorcery card from your graveyard. Put a number of " +
        "+1/+1 counters on this creature equal to half that card's mana value, rounded up."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.self.enters()
        description = "When this creature enters, exile an instant or sorcery card from your graveyard. Put " +
            "a number of +1/+1 counters on this creature equal to half that card's mana value, rounded up."
        effect = Effects.Pipeline {
            val spells = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.InstantOrSorcery),
            )
            val exiled = chooseExactly(
                1,
                from = spells,
                chooser = Chooser.Controller,
                prompt = "Exile an instant or sorcery card from your graveyard",
            )
            exile(exiled)
            run(
                Effects.AddDynamicCounters(
                    CounterType.PLUS_ONE_PLUS_ONE,
                    DynamicAmounts.manaValueOf(exiled) divRoundedUp 2,
                    EffectTarget.Self,
                )
            )
        }
    }

    modalBack("Flamethrower Sonata") {
        manaCost = "{1}{R}"
        typeLine = "Sorcery"
        oracleText = "Discard a card, then draw a card. When you discard an instant or sorcery card this " +
            "way, Flamethrower Sonata deals damage equal to that card's mana value to target creature or " +
            "planeswalker you don't control."
        imageUri = "https://cards.scryfall.io/normal/back/8/7/87463b68-3642-41c7-a11c-67d524759b60.jpg?1783927334"
        spell {
            effect = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val discarded = chooseExactly(
                    1,
                    from = hand,
                    chooser = Chooser.Controller,
                    prompt = "Discard a card",
                )
                discard(discarded)
                run(Effects.DrawCards(1))
                ifNotEmpty(discarded, filter = GameObjectFilter.InstantOrSorcery) {
                    run(
                        Effects.ReflexiveTrigger(
                            action = Effects.Nothing,
                            optional = false,
                            descriptionOverride = "Flamethrower Sonata deals damage equal to the discarded " +
                                "card's mana value to target creature or planeswalker you don't control",
                        ) {
                            val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
                            effect = Effects.DealDamage(DynamicAmounts.manaValueOf(discarded), victim)
                        }
                    )
                }
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87463b68-3642-41c7-a11c-67d524759b60.jpg?1783927334"
        ruling(
            "2021-04-16",
            "You choose which instant or sorcery card to exile as Torrent Sculptor's enters-the-battlefield " +
                "ability resolves. You must exile one if able.",
        )
        ruling(
            "2021-04-16",
            "Flamethrower Sonata goes on the stack without a target. While it is resolving, you discard a card, " +
                "then draw a card. When you discard an instant or sorcery card this way, the reflexive triggered " +
                "ability triggers and you pick a target to be dealt damage. This is different from effects that " +
                "say \"If you do . . .\" in that players can respond to the triggered ability knowing how much " +
                "damage will be dealt.",
        )
        ruling(
            "2021-04-16",
            "If you have no cards in hand as Flamethrower Sonata resolves, you won't be able to discard a card, " +
                "but you will draw a card. No damage will be dealt.",
        )
        ruling(
            "2021-04-16",
            "If a player casts a spell that targets multiple permanents their opponent controls with ward, each " +
                "of those ward abilities will trigger. If that player doesn't pay for all of them, the spell will " +
                "be countered.",
        )
    }
}
