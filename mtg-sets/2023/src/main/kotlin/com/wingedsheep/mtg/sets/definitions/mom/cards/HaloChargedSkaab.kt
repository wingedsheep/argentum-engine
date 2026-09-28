package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Halo-Charged Skaab — March of the Machine #60.
 * {4}{U} · Creature — Zombie · 4/4
 *
 * When this creature enters, each player mills two cards. Then you may put an instant, sorcery,
 * or battle card from your graveyard on top of your library.
 *
 * The graveyard is gathered after the mill, so a card milled this way is a legal choice. The
 * "you may" is the `ChooseUpTo(1)` selection.
 */
val HaloChargedSkaab = card("Halo-Charged Skaab") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, each player mills two cards. Then you may put an " +
        "instant, sorcery, or battle card from your graveyard on top of your library. (To mill " +
        "two cards, a player puts the top two cards of their library into their graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.Each)) then
            Effects.Pipeline {
                val candidates = gather(
                    CardSource.FromZone(
                        Zone.GRAVEYARD,
                        Player.You,
                        GameObjectFilter.InstantOrSorcery or GameObjectFilter.Battle
                    )
                )
                val chosen = chooseUpTo(
                    1,
                    from = candidates,
                    prompt = "Put an instant, sorcery, or battle card on top of your library"
                )
                toLibraryTop(chosen)
            }
        description = "When this creature enters, each player mills two cards. Then you may put an " +
            "instant, sorcery, or battle card from your graveyard on top of your library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Igor Kieryluk"
        flavorText = "Innistrad's stitchers were practiced at incorporating magical substances into undead creations."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c6bdac3-6762-41a4-821f-ab7034a823d9.jpg?1783917036"
    }
}
