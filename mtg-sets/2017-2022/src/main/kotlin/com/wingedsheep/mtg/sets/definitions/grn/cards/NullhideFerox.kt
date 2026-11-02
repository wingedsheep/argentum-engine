package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.ZoneChangeCause
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nullhide Ferox — Guilds of Ravnica #138 (canonical printing)
 * {2}{G}{G} · Creature — Beast · 6/6
 *
 * Hexproof
 * You can't cast noncreature spells.
 * {2}: This creature loses all abilities until end of turn. Any player may activate this ability.
 * If a spell or ability an opponent controls causes you to discard this card, put it onto the
 * battlefield instead of putting it into your graveyard.
 *
 * - The drawback is [PlayersCantCastSpells] turned inward (Grid Monitor's shape) with a
 *   noncreature spell filter.
 * - The {2} ability is an ordinary activated ability any player may activate
 *   ([ActivationRestriction.AnyPlayerMay], Ifh-Bíff Efreet's shape); losing all abilities removes
 *   hexproof, the cast restriction and the ability itself until end of turn.
 * - The discard clause is Obstinate Baloth's self-only [RedirectZoneChange] from hand, narrowed by
 *   [ZoneChangeCause.DiscardedByOpponentEffect].
 */
val NullhideFerox = card("Nullhide Ferox") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 6
    toughness = 6
    oracleText = "Hexproof\n" +
        "You can't cast noncreature spells.\n" +
        "{2}: This creature loses all abilities until end of turn. Any player may activate this ability.\n" +
        "If a spell or ability an opponent controls causes you to discard this card, put it onto the " +
        "battlefield instead of putting it into your graveyard."

    keywords(Keyword.HEXPROOF)

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.You,
            spellFilter = GameObjectFilter.Noncreature,
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.RemoveAllAbilities(EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.AnyPlayerMay)
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.BATTLEFIELD,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Any,
                from = Zone.HAND,
                to = Zone.GRAVEYARD
            ),
            selfOnly = true,
            requiredCause = ZoneChangeCause.DiscardedByOpponentEffect
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "138"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24c30bb0-06ba-432b-a20c-6fa79b0dc68a.jpg?1783934148"
        ruling(
            "2018-10-05",
            "Nullhide Ferox's activated ability can be activated only while it's on the battlefield. " +
                "Players can't, for example, activate it to try to stop its replacement effect from " +
                "putting it onto the battlefield."
        )
        ruling(
            "2018-10-05",
            "Once Nullhide Ferox's activated ability has resolved, it can gain new abilities. " +
                "It won't lose those abilities."
        )
        ruling(
            "2018-10-05",
            "Once Nullhide Ferox has lost its activated ability that makes it lose its abilities, " +
                "that ability can't be activated again until the turn's over."
        )
    }
}
