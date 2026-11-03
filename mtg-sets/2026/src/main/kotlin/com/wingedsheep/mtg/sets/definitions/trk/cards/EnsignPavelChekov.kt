package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ensign Pavel Chekov
 * {3}{U}
 * Legendary Creature — Human Pilot
 * 1/5
 *
 * Strange new worlds — When Ensign Chekov enters, each opponent exiles the top X cards of their
 * library, where X is the number of differently named lands you control. For each opponent, choose
 * a land card that player owns exiled this way. You may play the chosen cards for as long as they
 * remain exiled.
 *
 * "Strange new worlds" is an ability word with no rules meaning.
 *
 * `ForEachPlayer(EachOpponent)` rebinds `Player.You` to each visited opponent, so the gather reads
 * *their* library, while X ("lands **you** control"), the land choice and the play permission all
 * go through the references that survive the rebind: `Player.ControllerOfSource` for the count and
 * the permission, `Chooser.SourceController` for the pick (Ixhel, Scion of Atraxa; Winnowing).
 * Choosing per opponent inside the loop keeps each pick to the cards that player owns. "Choose a
 * land card" is mandatory — exactly one when any land was exiled, nothing when none was.
 */
val EnsignPavelChekov = card("Ensign Pavel Chekov") {
    manaCost = "{3}{U}"
    typeLine = "Legendary Creature — Human Pilot"
    power = 1
    toughness = 5
    oracleText = "Strange new worlds — When Ensign Chekov enters, each opponent exiles the top X cards of " +
        "their library, where X is the number of differently named lands you control. For each opponent, " +
        "choose a land card that player owns exiled this way. You may play the chosen cards for as long as " +
        "they remain exiled."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.Pipeline {
                val exiled = gather(
                    CardSource.TopOfLibrary(
                        count = DynamicAmounts.differentlyNamedLandsYouControl(Player.ControllerOfSource),
                        player = Player.You,
                    )
                )
                exile(exiled, Player.You)
                val chosen = chooseExactly(
                    count = 1,
                    from = exiled,
                    chooser = Chooser.SourceController,
                    filter = GameObjectFilter.Land,
                    prompt = "Choose a land card you may play for as long as it remains exiled",
                )
                run(
                    Effects.GrantMayPlayFromExile(
                        from = chosen,
                        expiry = MayPlayExpiry.Permanent,
                        recipient = EffectTarget.PlayerRef(Player.ControllerOfSource),
                    )
                )
            },
        )
        description = "Strange new worlds — When Ensign Chekov enters, each opponent exiles the top X cards " +
            "of their library, where X is the number of differently named lands you control. For each " +
            "opponent, choose a land card that player owns exiled this way. You may play the chosen cards " +
            "for as long as they remain exiled."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Gal Or"
        flavorText = "\"I took naturally to navigation, sir. After all, Russia invented the map.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a441c12e-1a2f-4b04-83dd-407bf6040efe.jpg?1792753698"
    }
}
