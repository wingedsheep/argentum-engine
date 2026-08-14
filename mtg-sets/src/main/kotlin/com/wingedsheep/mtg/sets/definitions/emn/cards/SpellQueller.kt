package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Spell Queller
 * {1}{W}{U}
 * Creature — Spirit
 * 2/3
 * Flash
 * Flying
 * When this creature enters, exile target spell with mana value 4 or less.
 * When this creature leaves the battlefield, the exiled card's owner may cast that card without
 * paying its mana cost.
 *
 * The ETB uses [Effects.ExileTargetSpell] with `linkToSource` — a *non-counter* exile (rulings:
 * "Spells that can't be countered can be exiled by Spell Queller's ability. They won't resolve.")
 * that also records the card in this creature's linked-exile pile, so the leaves trigger can find
 * it. Nothing is granted at that point.
 *
 * The leaves trigger is deliberately **not** a lingering may-play permission. The ruling is
 * explicit: "If the player casts the exiled card, they do so as part of the resolution of Spell
 * Queller's last ability. The player can't wait to cast it later in the turn. Timing permissions
 * based on the card's type are ignored." That is exactly
 * [Effects.CastFromCollectionWithoutPayingCost] — a synthesized cast during resolution, like
 * Cascade — wrapped in a [MayEffect] for "may".
 *
 * The chooser and caster is the exiled card's *owner*, not this creature's controller, so both run
 * inside [Effects.ForEachPlayer] over [Player.OwnersOfLinkedExile], which rebinds the controller to
 * that player for the nested effects (same construction as Unidentified Hovership). An empty pile —
 * the "Queller left before its enters ability resolved" case — yields no owners, so the trigger
 * correctly does nothing and the spell is then exiled forever.
 */
val SpellQueller = card("Spell Queller") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 3
    oracleText = "Flash\n" +
        "Flying\n" +
        "When this creature enters, exile target spell with mana value 4 or less.\n" +
        "When this creature leaves the battlefield, the exiled card's owner may cast that card " +
        "without paying its mana cost."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target("target spell with mana value 4 or less", Targets.SpellWithManaValueAtMost(4))
        effect = Effects.ExileTargetSpell(linkToSource = true)
        description = "When this creature enters, exile target spell with mana value 4 or less."
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ForEachPlayer(
            players = Player.OwnersOfLinkedExile,
            effects = listOf(
                GatherCardsEffect(source = CardSource.FromLinkedExile(), storeAs = "quelledCard"),
                MayEffect(
                    Effects.CastFromCollectionWithoutPayingCost("quelledCard"),
                    descriptionOverride = "Cast the exiled card without paying its mana cost"
                )
            )
        )
        description = "When this creature leaves the battlefield, the exiled card's owner may " +
            "cast that card without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b76bcd4-580a-4435-afe9-290940b1837f.jpg?1783937428"

        ruling("2025-01-24", "For spells with {X} in their mana costs, use the value chosen for X to determine if the spell's mana value is 4 or less.")
        ruling("2025-01-24", "Spells that can't be countered can be exiled by Spell Queller's ability. They won't resolve.")
        ruling("2025-01-24", "If the exiled card's owner casts it, the spell has no relation to the spell that player originally cast. Any choices made for the original spell or effects affecting the original spell aren't carried over to the new one.")
        ruling("2025-01-24", "If the player casts the exiled card, they do so as part of the resolution of Spell Queller's last ability. The player can't wait to cast it later in the turn. Timing permissions based on the card's type are ignored, but other restrictions (such as \"Cast [this card] only during combat\") are not.")
        ruling("2025-01-24", "If a player casts a card \"without paying its mana cost,\" they can't choose to cast it for any alternative costs, such as emerge costs. The player can, however, pay additional costs, such as escalate costs. If the card has any mandatory additional costs, those must be paid to cast the card.")
        ruling("2025-01-24", "If the card has {X} in its mana cost, the player must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2025-01-24", "If Spell Queller leaves the battlefield before its \"enters\" ability resolves, its leaves-the-battlefield triggered ability triggers, resolves, and does nothing. Then its first triggered ability resolves and exiles the spell forever.")
    }
}
