package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aquatic Alchemist // Bubble Up
 * {1}{U}
 * Creature — Elemental
 * 1/3
 *
 * Whenever you cast your first instant or sorcery spell each turn, this creature gets +2/+0
 * until end of turn.
 *
 * Adventure: Bubble Up — {2}{U}, Sorcery — Adventure
 * Put target instant or sorcery card from your graveyard on top of your library.
 *
 * "Your **first instant or sorcery** spell each turn" is a *single* combined counter, so the
 * intervening-if is one `YouCastFirstSpellOfTypeThisTurn(InstantOrSorcery)` — not the two separate
 * branches [AlaniaDivergentStorm][com.wingedsheep.mtg.sets.definitions.blb.cards.AlaniaDivergentStorm]
 * uses for "the first instant spell, the first sorcery spell". Casting an instant and then a sorcery
 * in the same turn pumps once here, twice on Alania.
 *
 * `triggerCondition` (not a plain filtered trigger) because CR 603.4 makes this an intervening "if":
 * it is checked both when the ability would trigger and again on resolution. That's the behaviour we
 * want — if the triggering spell somehow stops being the first matching spell before resolution, the
 * ability does nothing.
 *
 * The pump resolves before the spell that triggered it (the ability goes on the stack above it), so
 * the +2/+0 is live for anything the spell itself does — the usual "cast a burn spell, then block
 * profitably" line.
 *
 * The Adventure is the graveyard-to-top-of-library shape of
 * [WoodlandAcolyte]'s Mend the Wilds, narrowed to instants and sorceries via the pre-existing
 * [TargetFilter.InstantOrSorceryInYourGraveyard]-equivalent construction; `ownedByYou()` is what
 * "your graveyard" means (CR 404.1). With no instant or sorcery in your graveyard there is no legal
 * target, so Bubble Up can't be cast at all — and the creature half therefore never becomes castable
 * from exile that way.
 */
val AquaticAlchemist = card("Aquatic Alchemist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    oracleText = "Whenever you cast your first instant or sorcery spell each turn, this creature " +
        "gets +2/+0 until end of turn."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.YouCastSpell
        triggerCondition = Conditions.YouCastFirstSpellOfTypeThisTurn(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "Whenever you cast your first instant or sorcery spell each turn, this " +
            "creature gets +2/+0 until end of turn."
    }

    adventure("Bubble Up") {
        manaCost = "{2}{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Put target instant or sorcery card from your graveyard on top of your library. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val card = target(
                "target instant or sorcery card from your graveyard",
                TargetObject(
                    filter = TargetFilter(
                        baseFilter = GameObjectFilter.InstantOrSorcery.ownedByYou(),
                        zone = Zone.GRAVEYARD,
                    ),
                ),
            )
            effect = Effects.Move(
                target = card,
                destination = Zone.LIBRARY,
                placement = ZonePlacement.Top,
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6f03f21-aeb9-428b-9167-b2604919bdd8.jpg?1783915124"
    }
}
